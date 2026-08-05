package DBTest.DBTest.Where;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserMemberService {

    private final UserMemberRepository userRepository;

    public UserMemberService(UserMemberRepository userRepository) {
        this.userRepository = userRepository;
    }

    // 🚨 [장애 재현] 전체 테이블 락을 발생시키는 위험한 업데이트
    @Transactional
    public void dangerousMassUpdate() throws InterruptedException {
        System.out.println("[Thread A] 🚨 대량 업데이트 시작 (WHERE 절 없음) - 테이블 전체 락 획득!");
        userRepository.resetAllCashDangerously();

        // 락을 쥐고 있는 상태를 확인하기 위해 5초간 트랜잭션을 붙잡고 있음 (롱 트랜잭션 시뮬레이션)
        Thread.sleep(5000);
        System.out.println("[Thread A] 🚨 대량 업데이트 완료 및 커밋 (락 해제)");
    }

    // 🛡️ [해결 방안] 1,000건씩 쪼개서 업데이트 (Row Lock 범위 최소화)
    public void safeMassUpdateInChunks() throws InterruptedException {
        System.out.println("[Thread A] 🛡️ 청크 단위 안전한 대량 업데이트 시작");
        long lastId = 0L;
        int chunkSize = 1000;

        while (true) {
            // 1. WHERE 절(id > ?)을 이용하여 1,000건만 조회
            List<UserMember> users = userRepository.findByIdGreaterThanOrderByIdAsc(lastId, PageRequest.of(0, chunkSize));
            if (users.isEmpty()) break;

            // 2. 캐시 변경 (JPA 더티 체킹 준비)
            for (UserMember user : users) {
                user.setCash(0L);
            }

            // 3. 변경된 1000건을 저장 (내부적으로 짧은 트랜잭션 동작 -> 락 최소화)
            userRepository.saveAll(users);

            System.out.println("[Thread A] " + users.size() + "건 청크 업데이트 완료. (현재까지 ID: " + users.get(users.size() - 1).getId() + ")");
            lastId = users.get(users.size() - 1).getId();

            // 💡 다음 청크로 넘어가기 전 0.1초 대기 (이 틈에 다른 유저의 요청이 쏙 들어와서 정상 처리됩니다!)
            Thread.sleep(100);
        }
        System.out.println("[Thread A] 🛡️ 대량 업데이트 완료");
    }

    // 👤 일반 유저의 단건 업데이트 요청
    @Transactional
    public void updateSingleUserCash(Long id, Long amount) {
        UserMember user = userRepository.findById(id).orElseThrow();
        user.setCash(user.getCash() + amount);
        System.out.println("[Thread B] 👤 " + id + "번 유저 단건 업데이트 성공!");
    }
}
