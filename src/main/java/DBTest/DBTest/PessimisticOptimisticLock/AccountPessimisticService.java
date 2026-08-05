package DBTest.DBTest.PessimisticOptimisticLock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountPessimisticService {

    private final AccountRepository accountRepository;

    public AccountPessimisticService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void withDraw(Long accountId, Long amount) {
        // 1. SELECT ... FOR UPDATE 쿼리 발생 -> 데이터에 X-Lock을 검
        // 다른 스레드는 여기서 이전 스레드의 트랜잭션이 끝날 때까지 얌전히 대기합니다.
        Account account = accountRepository.findByIdWhitPessimisticLock(accountId)
                .orElseThrow();

        // 2. 출금 로직 실행
        account.withdraw(amount);

        // 3. 트랜잭션 종료 시 COMMIT 되며 안전하게 락이 해제됨
    }
}
