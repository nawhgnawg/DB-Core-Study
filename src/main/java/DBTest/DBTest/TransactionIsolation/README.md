# 📌 [Chapter 4] 트랜잭션 격리 수준 (Transaction Isolation Levels)

## 💡 핵심 요약 (Key Concept)

### 1. 트랜잭션의 딜레마
* **정합성(Consistency) vs 동시성(Concurrency)의 Trade-off**
* 병렬 처리를 극대화하면 이상 현상이 발생하고, 정합성을 완벽히 맞추면 락(Lock) 대기로 인해 성능이 마비됨.

### 2. 낮은 격리 수준의 3대 이상 현상 (Anomalies)
1. **Dirty Read (더티 리드):** 커밋되지 않은 타 트랜잭션의 변경 데이터를 미리 읽는 현상.
2. **Non-repeatable Read (반복 불가능한 읽기):** 동일한 쿼리를 두 번 실행했을 때, 타 트랜잭션의 Update/Commit으로 인해 결과가 달라지는 현상.
3. **Phantom Read (팬텀 리드):** 범위(Range) 조회를 두 번 실행했을 때, 타 트랜잭션의 Insert로 인해 없던 데이터가 나타나는 현상.

### 3. 격리 수준 4단계 (Isolation Levels)
1. **Read Uncommitted:** 격리성 무시. (Dirty Read 발생)
2. **Read Committed:** 커밋된 데이터만 읽음. (Non-repeatable Read 발생) - *Oracle 기본값*
3. **Repeatable Read:** 트랜잭션 내내 첫 스냅샷 유지. (Phantom Read 발생 가능) - *MySQL 기본값*
4. **Serializable:** 모든 읽기/쓰기에 완벽한 락 획득. (성능 최악, 제한적 사용)

---

## ⚖️ 격리 수준별 이상 현상 발생표

| 격리 수준 | Dirty Read | Non-repeatable Read | Phantom Read |
| :--- | :---: | :---: | :---: |
| **Read Uncommitted** | ⭕ | ⭕ | ⭕ |
| **Read Committed** | ❌ 방어 | ⭕ | ⭕ |
| **Repeatable Read** | ❌ 방어 | ❌ 방어 | ⭕ (MySQL은 ❌방어) |
| **Serializable** | ❌ 방어 | ❌ 방어 | ❌ 방어 |

---

## 🛡️ MVCC (Multi-Version Concurrency Control)
* InnoDB 등 최신 RDBMS가 락(Lock) 없이 읽기 일관성을 제공하는 핵심 아키텍처.
* 데이터를 읽을 때, 현재 테이블의 값이 내 트랜잭션 시점보다 최신이라면 **Undo Log(언두 로그)**에 저장된 과거 스냅샷 데이터를 조회하여 고립성을 보장함.

---

## 💫 면접 대비 Q&A

**Q. 트랜잭션 격리 수준이란 무엇이며, 왜 무조건 최고 수준(SERIALIZABLE)으로 설정하지 않나요?**
> **A.** 격리 수준은 동시 트랜잭션이 서로의 변경 데이터를 어디까지 볼 수 있는지를 결정하는 정책입니다. 낮은 수준은 처리량은 높지만 정합성 오류가 발생하고, 높은 수준은 락 병목으로 성능이 저하됩니다. 실무에서는 성능을 위해 Read Committed를 기본으로 쓰고, 정산처럼 일관성이 중요한 구간에서만 Repeatable Read 기반에 비관적 락(`SELECT ... FOR UPDATE`)을 조합해 필요한 행만 명시적으로 잠급니다.

**Q. 왜 Oracle은 Read Committed를, MySQL은 Repeatable Read를 기본으로 쓰나요?**
> **A.** Oracle은 SELECT 문 단위로 스냅샷을 새로 떠서 문 단위 정합성을 보장하는 철학이라 Read Committed로도 충분했지만, MySQL InnoDB는 바이너리 로그 기반 복제 시 트랜잭션 전체가 일관된 스냅샷을 유지해야 안전했기 때문에 역사적으로 Repeatable Read를 기본값으로 채택했습니다.

**Q. Repeatable Read에서 Phantom Read가 발생하는 결정적인 이유는?**
> **A.** ANSI 표준상 Repeatable Read는 '이미 읽은 행'의 값 불변만 보장할 뿐 '새로 삽입된 행'까지 막지는 않기 때문입니다. 다만 MySQL InnoDB는 Next-Key Lock(레코드 락 + 갭 락)으로 삽입 자체를 막아버려서, 실무에서는 대부분 Phantom Read가 발생하지 않습니다.