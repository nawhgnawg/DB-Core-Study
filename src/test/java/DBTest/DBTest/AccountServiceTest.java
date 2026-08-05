package DBTest.DBTest;

import DBTest.DBTest.PessimisticOptimisticLock.Account;
import DBTest.DBTest.PessimisticOptimisticLock.AccountPessimisticService;
import DBTest.DBTest.PessimisticOptimisticLock.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AccountServiceTest {

    @Autowired
    private AccountPessimisticService accountService;

    @Autowired
    private AccountRepository accountRepository;

    private Long savedAccountId;

    @BeforeEach
    void setUp() {
        // 테스트 시작 전, 잔액이 10,000원인 계좌를 하나 생성합니다.
        Account account = new Account();
        account.deposit(10000L);
        savedAccountId = accountRepository.save(account).getId();
    }

    @Test
    @DisplayName("비관적 락 적용: 100명이 동시에 100원씩 출금하면 잔액은 0원이 되어야 한다.")
    void withdrawWithPessimisticLock() throws InterruptedException {
        // 1. 동시에 실행할 요청 수 (100번)
        int threadCount = 100;

        // 2. 멀티스레드 환경 구축 (32개의 스레드가 일할 준비)
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        // 3. CountDownLatch: 100개의 요청이 모두 끝날 때까지 메인 스레드를 기다리게 해주는 동기화 도구
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 4. 100번의 출금 요청을 스레드 풀에 던짐
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    // 각 스레드가 100원씩 출금 시도!
                    accountService.withDraw(savedAccountId, 100L);
                } catch (Exception e) {
                    System.err.println("스레드 내부 에러 발생: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    // 처리가 끝나면 카운트를 1씩 감소
                    latch.countDown();
                }
            });
        }

        // 5. 100개의 요청이 모두 끝날 때까지 대기
        latch.await();

        // 6. 결과 검증 (Assert)
        Account finalAccount = accountRepository.findById(savedAccountId).orElseThrow();
        System.out.println("최종 잔액: " + finalAccount.getBalance() + "원");

        // 10,000원에서 100원씩 100번 뺐으니 0원이어야 정상!
        assertThat(finalAccount.getBalance()).isEqualTo(0L);

    }

}
