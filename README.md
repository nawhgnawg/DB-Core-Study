# 📚 데이터베이스 핵심 총정리 & 실전 테스트 (DB Core Study)

> "이론으로만 알던 DB 지식, 코드로 직접 터뜨려보고 우아하게 해결합니다."

이 저장소는 유튜브 **[코딩하는기술사 - 개발자가 알아야 할 데이터베이스 핵심 총정리]** 영상을 기반으로, 실무에서 마주칠 수 있는 DB/CS 핵심 개념을 공부하고 기록한 공간입니다.
단순한 이론 요약을 넘어 **Spring Data JPA 환경에서의 동시성 제어, 트러블슈팅, 아키텍처 딥다이브 테스트 코드**를 직접 구현하고 검증합니다.

## 🛠️ Tech Stack
- **Language:** Java 17
- **Framework:** Spring Boot, Spring Data JPA
- **Database:** H2 Database (In-Memory for Test), MySQL
- **Test:** JUnit5, ExecutorService (Multi-threading)

## 📑 목차 (Table of Contents)

| Chapter                                                                                       | Topic | Key Interview Keywords |
|:----------------------------------------------------------------------------------------------| :--- | :--- |
| **[01-deadlock-livelock](./src/main/java/DBTest/DBTest/DeadLiveLock/README.md)**              | 데드락과 라이브락 | `교착상태 4가지 조건`, `Lock Ordering`, `Livelock & Random Backoff` |
| **[02-pessimistic-vs-optimistic-lock](./src/main/java/DBTest/DBTest/DeadLiveLock/README.md)** | 비관적 락 vs 낙관적 락 | `Lost Update`, `SELECT FOR UPDATE`, `@Version`, `Trade-off` |
| **[03-update-without-where-lock](./src/main/java/DBTest/DBTest/DeadLiveLocke/README.md)**     | WHERE 절 누락과 Table Lock | `Full Scan & Index Lock`, `Lock Wait Timeout`, `Chunking Strategy` |
| **[04-transaction-isolation](./src/main/java/DBTest/DBTest/TransactionIsolation/README.md)** | 트랜잭션 격리 수준 | `Read Committed`, `Repeatable Read`, `MVCC & Undo Log`, `Phantom Read` |

## 💡 학습 목표
1. **눈으로 확인하는 CS:** 모든 이론은 직접 JUnit 멀티스레드 테스트 코드를 작성하여 눈으로 검증합니다.
2. **아키텍처 딥다이브:** 에러 로그를 한 줄 한 줄 분석하여 프레임워크와 DB 락 매니저의 내부 동작 원리를 파악합니다.
3. **실무적 방어:** "장애가 왜 났을까?"에 그치지 않고, "어떻게 방어 코드를 짤 것인가?"를 고민합니다.