package DBTest.DBTest.PessimisticOptimisticLock;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 2. 레포지토리(Repository): 비관적 락 설정
 * 비관적 락은 데이터를 읽어올 때(SELECT)부터 락을 걸어야 하므로, Repository 단계에서 어노테이션을 통해 설정합니다.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // 🔒 비관적 락 (Pessimistic Write Lock)
    // 이 메서드를 호출하면 DB에 'SELECT ... FOR UPDATE' 쿼리가 날아갑니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWhitPessimisticLock(@Param("id") Long id);

    // 낙관적 락은 일반 조회 메서드를 그대로 사용해도 됩니다.
    Optional<Account> findById(Long id);
}
