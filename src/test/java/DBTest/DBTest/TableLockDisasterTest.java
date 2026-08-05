package DBTest.DBTest;

import DBTest.DBTest.UpdateWithoutWhere.UserMember;
import DBTest.DBTest.UpdateWithoutWhere.UserMemberRepository;
import DBTest.DBTest.UpdateWithoutWhere.UserMemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class TableLockDisasterTest {

    @Autowired
    private UserMemberService userService;
    @Autowired
    private UserMemberRepository userRepository;

    @BeforeEach
    void setUp() {
        // 테스트 전 5,000명의 유저 생성
        for (int i = 0; i < 5000; i++) {
            userRepository.save(new UserMember());
        }
    }

    @Test
    @DisplayName("테스트 1: WHERE 절 없는 업데이트 (장애 발생 예상)")
    void test1_DangerousTableLock() throws InterruptedException {
        System.out.println("\n=== 🚨 테스트 1: WHERE 절 없는 업데이트 (장애 발생 예상) ===");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // Thread A: 전체 유저 캐시 0원으로 초기화 (WHERE 절 없음 -> 5초 소요)
        executor.submit(() -> {
            try {
                userService.dangerousMassUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        Thread.sleep(500);      // Thread A가 락을 선점할 시간 부여

        // Thread B: 불쌍한 5번 유저가 500원을 충전하려 시도함
        executor.submit(() -> {
            try {
                System.out.println("[Thread B] 👤 5번 유저 업데이트 요청 (대기 중...)");
                userService.updateSingleUserCash(5L, 500L);
            } catch (Exception e) {
                // 💥 락 대기 시간(2초)을 초과하여 LockTimeoutException 발생! (유저 전원 튕김 현상)
                System.err.println("[Thread B] 💥 에러 발생: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        latch.await();
    }

    @Test
    @DisplayName("테스트 2: 청크 단위 안전한 업데이트")
    void test2_SafeChunkUpdate() throws InterruptedException {
        System.out.println("\n=== 🛡️ 테스트 2: 청크 단위 안전한 업데이트 ===");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // Thread A: 1,000건씩 쪼개서 업데이트 (트랜잭션이 짧게 짧게 끊어짐)
        executor.submit(() -> {
            try {
                userService.safeMassUpdateInChunks();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        Thread.sleep(500); // Thread A가 작업을 시작할 시간 부여

        // Thread B: 불쌍한 5번 유저가 500원을 충전하려 시도함
        executor.submit(() -> {
            try {
                System.out.println("[Thread B] 👤 5번 유저 업데이트 요청");
                userService.updateSingleUserCash(5L, 500L);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        latch.await();
    }

}
