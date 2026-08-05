package DBTest.DBTest.PessimisticOptimisticLock;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class OptimisticLockFacade {

    private final AccountOptimisticService accountOptimisticService;

    public OptimisticLockFacade(AccountOptimisticService accountOptimisticService) {
        this.accountOptimisticService = accountOptimisticService;
    }

    public void withdrawWithRetry(Long accountId, Long amount) throws InterruptedException {
        int retryCount = 0;

        while (true) {
            try {
                accountOptimisticService.withdraw(accountId, amount);
                break;  // 성공하면 루프 탈출!
            } catch (ObjectOptimisticLockingFailureException e) {
                // 🚨 충돌 발생! (누군가 먼저 버전을 올렸음)
                retryCount++;
                System.out.println("버전 충돌 발생! 재시도 횟수: " + retryCount);

                // 50ms 대기 후 루프의 처음으로 돌아가 최신 버전의 데이터를 다시 읽어옵니다.
                Thread.sleep(50);
            }
        }
    }
}
