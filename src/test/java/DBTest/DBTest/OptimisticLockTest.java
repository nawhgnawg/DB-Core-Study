package DBTest.DBTest;

import DBTest.DBTest.PessimisticOptimisticLock.Account;
import DBTest.DBTest.PessimisticOptimisticLock.AccountRepository;
import DBTest.DBTest.PessimisticOptimisticLock.OptimisticLockFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class OptimisticLockTest {

    @Autowired
    private OptimisticLockFacade optimisticLockFacade;

    @Autowired
    private AccountRepository accountRepository;

    private Long savedAccountId;

    @BeforeEach
    void setUp() {
        Account account = new Account();
        account.deposit(10000L); // 초기 잔액 10,000원
        savedAccountId = accountRepository.save(account).getId();
    }

    @Test
    @DisplayName("낙관적 락 적용: 100명이 동시에 100원씩 출금 시도 시, 충돌과 재시도를 거쳐 0원이 되어야 한다.")
    void withdrawWithOptimisticLock() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // Facade의 재시도 로직을 호출!
                    optimisticLockFacade.withdrawWithRetry(savedAccountId, 100L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Account finalAccount = accountRepository.findById(savedAccountId).orElseThrow();
        System.out.println("최종 잔액: " + finalAccount.getBalance() + "원");
        System.out.println("최종 버전: " + finalAccount.getVersion());

        assertThat(finalAccount.getBalance()).isEqualTo(0L);
    }
}
