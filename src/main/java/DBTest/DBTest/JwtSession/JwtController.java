package DBTest.DBTest.JwtSession;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Key;
import java.security.SignatureException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@RestController
@RequestMapping("/api/jwt")
public class JwtController {

    // 서버만 아는 비밀키 (실무에서는 application.yml에서 주입받아 사용)
    private final String SECRET = "my-32-character-ultra-secure-and-ultra-long-secret";
    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    // ★ 실무의 핵심: Redis를 조작하기 위한 템플릿 객체
    private final StringRedisTemplate redisTemplate;

    public JwtController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 1. 로그인 (토큰 발급 및 Redis 저장)
    @PostMapping("/login")
    public Map<String, String> login(@RequestParam("userId") String userId) {
        long accessTokenValidTime = 1000 * 60 * 30; // 30분
        long refreshTokenValidTime = 1000 * 60 * 60 * 24 * 14; // 14일

        String accessToken = createToken(userId, accessTokenValidTime);
        String refreshToken = createToken(userId, refreshTokenValidTime);

        // ★ Redis에 Refresh Token 저장 (Key, Value, 만료시간, 시간단위)
        // 만료 시간(14일)이 지나면 Redis가 알아서 이 데이터를 삭제해 버립니다! (메모리 관리 끝판왕)
        redisTemplate.opsForValue().set(
                "RT:" + userId,       // Key: 구분을 위해 앞에 "RT:" 접두사를 붙이는 것이 관례
                refreshToken,         // Value: 실제 토큰 문자열
                14,                   // Time: 14
                TimeUnit.DAYS         // Unit: 일(Days)
        );

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        return tokens;
    }

    // 2. Access Token 재발급 (/refresh)
    @PostMapping("/refresh")
    public String refresh(@RequestHeader("Refresh-Token") String refreshToken) {
        try {
            // 1. 넘어온 Refresh Token 검증
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();

            String userId = claims.getSubject();

            // 2. ★ Redis에서 해당 유저의 Refresh Token을 꺼내옴
            String savedToken = redisTemplate.opsForValue().get("RT:" + userId);

            // 3. 보안 검증: Redis에 토큰이 없거나(만료/로그아웃), 넘어온 토큰과 다르면 해킹 시도로 간주
            if (savedToken == null) {
                throw new IllegalArgumentException("만료되었거나 로그아웃된 사용자입니다.");
            }
            if (!savedToken.equals(refreshToken)) {
                // 해킹 의심 상황! 보안을 위해 기존 토큰마저 삭제해버리는 것이 좋습니다.
                redisTemplate.delete("RT:" + userId);
                throw new IllegalArgumentException("토큰이 탈취되거나 변조되었습니다. 강제 로그아웃 처리합니다.");
            }

            // 4. 검증 통과 시 새로운 Access Token 30분짜리 발급
            return createToken(userId, 1000 * 60 * 30);

        } catch (JwtException e) {
            throw new RuntimeException("유효하지 않은 Refresh Token입니다.");
        }
    }

    // 3. 로그아웃
    @PostMapping("/logout")
    public String logout(@RequestParam("userId") String userId) {
        // ★ Redis에서 해당 유저의 Refresh Token 데이터를 즉시 삭제 (강제 로그아웃 효과)
        Boolean isDeleted = redisTemplate.delete("RT:" + userId);

        if (Boolean.TRUE.equals(isDeleted)) {
            return "로그아웃 성공. Redis에서 토큰 삭제됨.";
        } else {
            return "이미 로그아웃 되었거나 세션이 만료되었습니다.";
        }
    }

    // 4. 프로필 조회 (Access Token 검증)
    @GetMapping("/profile")
    public String getProfile(@RequestHeader("Authorization") String authHeader) {

        // 1. Bearer 규격 확인
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("토큰 형식이 잘못되었거나 없습니다.");
        }

        // 2. "Bearer " 부분(7글자)을 잘라내고 실제 토큰 문자열만 추출
        String token = authHeader.substring(7);

        try {
            // 3. 토큰 분해 및 위조/만료 여부 검증
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key) // 우리가 만든 비밀키로 서명 확인
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 4. 검증을 통과했다면 토큰 안에 들어있던 userId(Subject)를 꺼냄
            String userId = claims.getSubject();

            return userId + "님의 프로필 인증에 성공했습니다! 🎉";

        } catch (ExpiredJwtException e) {
            // 프론트엔드에게 "토큰 만료됐으니 Refresh API로 재발급 받아라"고 알려주는 에러
            throw new RuntimeException("Access Token이 만료되었습니다. (재발급 필요)");
        } catch (JwtException e) {
            // 서명이 다르거나 내용이 조작된 경우
            throw new RuntimeException("위조되었거나 유효하지 않은 토큰입니다.");
        }
    }

    // 토큰 생성 공통 메서드
    private String createToken(String userId, long expireTime) {
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", "USER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }



/*
    // 1. 로그인 (JWT 토큰 발급)
    @PostMapping("/login")
    public String login(@RequestParam("userId") String userId) {
        long expireTime = 1000 * 60 * 60; // 1시간

        return Jwts.builder()
                .setSubject(userId)           // 토큰 용도 (보통 사용자 ID)
                .claim("role", "USER")        // 추가 정보 (권한 등)
                .setIssuedAt(new Date())      // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + expireTime)) // 만료 시간
                .signWith(key, SignatureAlgorithm.HS256) // 알고리즘 및 비밀키로 서명
                .compact(); // 토큰 생성
    }

    // 2. 인증 필요 로직 (토큰 검증 및 다양한 예외 처리)
    @GetMapping("/profile")
    public String getProfile(@RequestHeader("Authorization") String authHeader) {
        // 예외 1: 헤더가 없거나 형식이 틀림
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("토큰이 없거나 잘못된 형식입니다.");
        }

        String token = authHeader.substring(7); // "Bearer " 이후의 실제 토큰 추출

        try {
            // 토큰을 파싱하면서 서명이 맞는지, 만료되었는지 자동으로 검증함
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);

            return userId + "님(권한:" + role + ")의 프로필입니다.";

            // 예외 2: 토큰 유효기간이 지남 (가장 빈번함)
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("토큰이 만료되었습니다. 다시 로그인해주세요.");

            // 예외 3: 서명이 다르거나 토큰이 위조됨 (누군가 페이로드를 조작함)
        } catch (SecurityException | MalformedJwtException e) {
            throw new RuntimeException("위조되거나 변조된 토큰입니다.");

            // 예외 4: 지원하지 않는 토큰 형식
        } catch (UnsupportedJwtException e) {
            throw new RuntimeException("지원하지 않는 형식의 토큰입니다.");
        }
    }
*/
}
