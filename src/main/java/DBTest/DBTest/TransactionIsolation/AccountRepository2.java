package DBTest.DBTest.TransactionIsolation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository2 extends JpaRepository<Account2, Long> {
}
