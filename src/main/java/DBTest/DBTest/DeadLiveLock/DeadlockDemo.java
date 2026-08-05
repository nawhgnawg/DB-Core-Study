package DBTest.DBTest.DeadLiveLock;

public class DeadlockDemo {
    private static final Object Resource1 = new Object();
    private static final Object Resource2 = new Object();

    public static void main(String[] args) {
        // 스레드 A: Resource1을 잡고, Resource2를 기다림
        Thread threadA = new Thread(() -> {
            // 1. 스레드 A가 Resource1의 열쇠를 달라고 JVM에게 요청
            // 2. 아무도 안 쓰고 있다면 열쇠를 얻고 블록 { 안으로 진입
            // 3. 만약 누군가 쓰고 있다면, 열쇠가 반납될 때까지 여기서 실행을 멈추고 기다림
            synchronized (Resource1) {
                System.out.println("Thread A: Resource1 획득");
                try { Thread.sleep(50); } catch (InterruptedException e) { } // 충돌 유도

                System.out.println("Thread A: Resource2 획득 대기 중...");
                synchronized (Resource2) {
                    System.out.println("Thread A: 모든 자원 획득 성공");
                }
            }
        });

        // 스레드 A: Resource1을 잡고, Resource2를 기다림
        Thread threadB = new Thread(() -> {
            synchronized (Resource2) {
                System.out.println("Thread B: Resource2 획득");
                try { Thread.sleep(50); } catch (InterruptedException e) { } // 충돌 유도

                System.out.println("Thread B: Resource1 획득 대기 중...");
                synchronized (Resource1) {
                    System.out.println("Thread B: 모든 자원 획득 성공");
                }
            }
        });

        threadA.start();
        threadB.start();
    }

}
