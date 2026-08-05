package DBTest.DBTest.DeadLiveLock;

public class DatabaseDeadlockDetectionDemo {

    // 1. Spring의 DeadlockLoserDataAccessException을 흉내 낸 커스텀 예외
    static class SimulatedDeadlockException extends RuntimeException {
        public SimulatedDeadlockException(String message) {
            super(message);
        }
    }

    public static void main(String[] args) {
        DatabaseDeadlockDetectionDemo demo = new DatabaseDeadlockDetectionDemo();
        System.out.println("=== 클라이언트: 송금 요청 ===");
        demo.transferMoney();
    }

    public void transferMoney() {
        int retryCount = 0;
        int maxRetries = 3;

        while (retryCount < maxRetries) {
            try {
                // DB 송금 트랜잭션 실행 시도
                executeTransferTransaction(retryCount);

                System.out.println("✅ 송금 성공! 작업을 정상 종료합니다.");
                return; // 성공하면 즉시 종료

            } catch (SimulatedDeadlockException e) {
                // 👈 핵심: DB 엔진이 데드락을 감지하고 현재 트랜잭션을 Kill 했을 때 이를 잡아냄!
                retryCount++;
                System.out.println("🚨 [에러 발생] DB가 데드락을 감지했습니다! 현재 트랜잭션이 강제 롤백되었습니다.");
                System.out.println("🔄 " + retryCount + "번째 트랜잭션 재시도를 준비합니다...\n");

                try {
                    Thread.sleep(500); // DB 락이 풀릴 시간을 잠시 벌어줌 (0.5초 대기)
                } catch (InterruptedException ie) {}
            }
        }
        System.out.println("❌ 최대 재시도 초과. 고객에게 '서버 혼잡' 안내 메시지를 발송합니다.");
    }

    // 2. 실제 DB 송금 쿼리를 날리는 상황을 흉내 낸 가상의 메서드
    private void executeTransferTransaction(int retryCount) {
        System.out.println("▶️ DB 트랜잭션 시작... (송금 쿼리 실행 중)");

        // 상황 가정: 0번째(첫) 시도에서는 운 나쁘게 DB 내부에서 데드락이 엮임
        if (retryCount == 0) {
            System.out.println("💥 (DB 내부 상황: 데드락 사이클 감지! 희생양으로 선정되어 롤백됨)");
            // DB가 던진 데드락 에러를 Java 예외로 발생시킴
            throw new SimulatedDeadlockException("Deadlock found when trying to get lock");
        }

        // 1번째 재시도에서는 데드락이 풀려있으므로 정상 통과
        System.out.println("▶️ DB 트랜잭션 커밋 완료!");
    }
}