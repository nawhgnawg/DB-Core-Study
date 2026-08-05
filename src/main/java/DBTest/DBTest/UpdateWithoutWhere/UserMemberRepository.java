package DBTest.DBTest.UpdateWithoutWhere;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserMemberRepository extends JpaRepository<UserMember, Long> {

    // 🚨 1. 장애를 유발하는 위험한 쿼리 (WHERE 절 없음 -> 풀 스캔 & 전체 락)
    // 호출하는 순간 DB 전체에 락이 걸리며 서비스가 마비될 수 있습니다.
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserMember u SET u.cash = 0")
    void resetAllCashDangerously();

    // 🛡️ 2. 안전한 처리를 위한 청크(Chunk) 조회 쿼리 (인덱스 사용)
    List<UserMember> findByIdGreaterThanOrderByIdAsc(Long id, Pageable pageable);

}
