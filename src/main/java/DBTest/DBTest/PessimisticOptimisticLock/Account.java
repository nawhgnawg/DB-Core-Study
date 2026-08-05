package DBTest.DBTest.PessimisticOptimisticLock;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 1. 엔티티(Entity) 설계: 낙관적 락을 위한 준비
 * 낙관적 락을 사용하려면 엔티티에 버전을 관리할 필드가 필요합니다.
 * 비관적 락은 DB 자체의 기능을 쓰기 때문에 별도의 필드가 필요 없습니다.
 */
@Entity
@Getter
@Setter
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long balance = 0L;;

    // 낙관적 락의 핵심!
    // JPA가 업데이트 시 자동으로 버전을 관리해주도록 @Version 어노테이션을 붙입니다.
    @Version
    private Long version;

    public void withdraw(Long amount) {
        if (this.balance < amount) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }
        this.balance -= amount;
    }

    public void deposit(Long amount) {
        this.balance += amount;
    }
}
