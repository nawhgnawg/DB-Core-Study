package DBTest.DBTest.TransactionIsolation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository2 accountRepository;

    public AccountService(AccountRepository2 accountRepository) {
        this.accountRepository = accountRepository;
    }

    // 🚨 [재현용] Read Uncommitted - 커밋 전 값을 그대로 노출
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Long readBalanceUncommitted(Long id) {
        return accountRepository.findById(id).orElseThrow().getBalance();
    }

    // 🛡️ [기본 권장] Read Committed - 실무에서 가장 널리 쓰는 기본값
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Long readBalanceCommitted(Long id) {
        return accountRepository.findById(id).orElseThrow().getBalance();
    }

    // 🛡️ [정합성 강화] Repeatable Read - 트랜잭션 내내 같은 스냅샷 유지
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Long readTwiceRepeatable(Long id, long sleepMillis) throws InterruptedException {
        Long first = accountRepository.findById(id).orElseThrow().getBalance();
        Thread.sleep(sleepMillis);  // 그 사이 다른 트랜잭션이 값을 바꾸고 커밋함
        Long second = accountRepository.findById(id).orElseThrow().getBalance();
        System.out.println("[Repeatable Read] 첫 조회=" + first + ", 재조회=" + second);
        return second;
    }

    // 👤 값을 바꾸고 커밋하는 트랜잭션 (다른 스레드에서 실행)
    @Transactional
    public void updateBalance(Long id, Long newBalance) {
        Account2 account2 = accountRepository.findById(id).orElseThrow();
        account2.setBalance(newBalance);
    }

    // 🚨 커밋 전 값을 sleep으로 붙잡고 있다가 롤백하는 트랜잭션 (Dirty Read 재현용)
    @Transactional
    public void updateThenRollback(Long id, Long tempBalance, long sleepMillis) throws InterruptedException {
        Account2 account2 = accountRepository.findById(id).orElseThrow();
        account2.setBalance(tempBalance);
        accountRepository.saveAndFlush(account2); // 커밋 전 DB에 값만 반영
        Thread.sleep(sleepMillis);
        throw new RuntimeException("의도적인 롤백 트리거"); // @Transactional에 의해 롤백됨
    }

}
