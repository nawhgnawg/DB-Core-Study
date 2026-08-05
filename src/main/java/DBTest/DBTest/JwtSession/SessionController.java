package DBTest.DBTest.JwtSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    // 1. 로그인 (세션 생성)
    @PostMapping("/login")
    public String login(@RequestParam("userId") String userId, HttpServletRequest request) {
        // ID/PW 검증 성공 가정

        // request.getSession(true): 세션이 없으면 새로 생성
        HttpSession session = request.getSession(true);
        session.setAttribute("LOGIN_USER", userId);
        session.setMaxInactiveInterval(3600);       // 예외 방지: 1시간 지나면 세션 자동 만료 (메모리 누수 방지)

        return "로그인 성공. Session ID: " + session.getId();
    }

    // 2. 인증 필요 로직 (예외 상황 처리 포함)
    @GetMapping("/profile")
    public String getProfile(HttpServletRequest request) {
        // request.getSession(false): 세션이 없으면 null 반환 (새로 생성하지 않음)
        HttpSession session = request.getSession(false);

        // 예외 1: 세션 자체가 없음 (로그인 안 했거나 만료됨)
        if (session == null) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다. (세션 없음)");
        }

        String userId = (String) session.getAttribute("LOGIN_USER");

        // 예외 2: 세션은 있으나 로그인 정보가 없음 (비정상적인 접근)
        if (userId == null) {
            throw new IllegalArgumentException("유효하지 않은 세션입니다.");
        }

        return userId + "님의 프로필입니다.";
    }

    // 3. 로그아웃 (세션 파기)
    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // 즉시 세션 파기
        }
        return "로그아웃 되었습니다.";
    }
}
