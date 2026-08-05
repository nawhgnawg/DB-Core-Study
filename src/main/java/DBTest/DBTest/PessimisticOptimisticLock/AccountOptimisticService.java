package DBTest.DBTest.PessimisticOptimisticLock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AccountOptimisticService {

    private final AccountRepository accountRepository;

    public AccountOptimisticService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void withdraw(Long accountId, Long amount) {
        // 비관적 락과 달리 일반 findById를 사용합니다.
        Account account = accountRepository.findById(accountId).orElseThrow();
        account.withdraw(amount);
        // 트랜잭션이 끝나는 시점에 JPA가 자동으로 UPDATE ... WHERE id=? AND version=? 쿼리를 날립니다.
    }
}
