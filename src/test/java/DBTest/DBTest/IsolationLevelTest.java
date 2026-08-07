package DBTest.DBTest;

import DBTest.DBTest.TransactionIsolation.Account2;
import DBTest.DBTest.TransactionIsolation.AccountRepository2;
import DBTest.DBTest.TransactionIsolation.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootTest
public class IsolationLevelTest {

    @Autowired
    private AccountRepository2 accountRepository;

    @Autowired
    private AccountService accountService;

    private Long accountId;

    @BeforeEach
    void setUp() {
        Account2 account2 = accountRepository.save(new Account2(100L));
        accountId = account2.getId();
    }

    @Test
    @DisplayName("테스트 1: READ_UNCOMMITTED는 Dirty Read를 그대로 노출한다")
    void test1_DirtyReadExposed() throws InterruptedException {
        System.out.println("\n=== 🚨 테스트 1: Dirty Read 재현 ===");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicLong dirtyValue = new AtomicLong();

        // Thread A: 50으로 바꾼 뒤 500ms 대기하다가 의도적으로 롤백
        executor.submit(() -> {
            try {
                accountService.updateThenRollback(accountId, 50L, 500);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        Thread.sleep(200);  // Thread A가 값을 바꿀 시간 확보

        // Thread B: 커밋 전인데도 READ_UNCOMMITTED로 읽어버림
        executor.submit(() -> {
            try {
                Long balance = accountService.readBalanceUncommitted(accountId);
                dirtyValue.set(balance);
                System.out.println("[Thread B] 💥 커밋 전 값을 읽음 (Dirty Read): " + balance);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        latch.await();

        // 롤백 이후 실제 DB 값은 100으로 복구되어야 함
        Long finalBalance = accountRepository.findById(accountId).orElseThrow().getBalance();
        System.out.println("[검증] Dirty Read 값=" + dirtyValue.get() + " / 롤백 후 실제 값=" + finalBalance);
    }


    @Test
    @DisplayName("테스트 2: REPEATABLE_READ는 같은 트랜잭션 안에서 스냅샷을 유지한다")
    void test2_RepeatableReadKeepsSnapshot() throws InterruptedException {
        System.out.println("\n=== 🛡️ 테스트 2: Repeatable Read 스냅샷 유지 ===");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        // Thread A: 첫 조회 후 500ms 대기했다가 다시 조회 (그 사이값이 바뀌어도 스냅샷 유지되어야 함)
        executor.submit(() -> {
            try {
                accountService.readTwiceRepeatable(accountId, 500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                latch.countDown();
            }
        });

        Thread.sleep(150); // Thread A가 첫 조회를 마칠 시간 확보

        // Thread B: 그 사이 값을 50으로 바꾸고 커밋
        executor.submit(() -> {
            try {
                accountService.updateBalance(accountId, 50L);
                System.out.println("[Thread B] 👤 잔액을 50으로 변경하고 커밋 완료");
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        // 콘솔에서 "첫 조회=100, 재조회=100" 이면 Repeatable Read 스냅샷이 정상 동작한 것
    }

}
