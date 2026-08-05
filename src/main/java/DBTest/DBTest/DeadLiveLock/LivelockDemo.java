package DBTest.DBTest.DeadLiveLock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LivelockDemo {

    private static final Lock lock1 = new ReentrantLock();
    private static final Lock lock2 = new ReentrantLock();

    public static void main(String[] args) {
        Thread threadA = new Thread(() -> {
            while (true) {
                if (lock1.tryLock()) {
                    System.out.println("Thread A: Lock 1 획득");
                    try { Thread.sleep(10); } catch (InterruptedException e) {} // 작업 유도

                    if (lock2.tryLock()) {
                        System.out.println("Thread A: Lock 2 획득 성공");
                        lock2.unlock();
                        lock1.unlock();
                        break;
                    }
                    // Lock 2를 못 잡으면 잡고 있던 Lock 1을 양보(해제)하고 재시도
                    System.out.println("Thread A: Lock 2 실패로 인해 Lock 1 양보 및 재시도...");
                    lock1.unlock();
                }
                try { Thread.sleep(10); } catch (InterruptedException e) {} // 재시도 간격
            }
        });

        Thread threadB = new Thread(() -> {
            while (true) {
                if (lock2.tryLock()) {
                    System.out.println("Thread B: Lock 2 획득.");
                    try { Thread.sleep(10); } catch (InterruptedException e) {}

                    if (lock1.tryLock()) {
                        System.out.println("Thread B: Lock 1 획득! 성공!");
                        lock1.unlock();
                        lock2.unlock();
                        break;
                    }
                    // Lock 1을 못 잡으면 잡고 있던 Lock 2를 양보(해제)하고 재시도
                    System.out.println("Thread B: Lock 1 실패로 인해 Lock 2 양보 및 재시도...");
                    lock2.unlock();
                }
                try { Thread.sleep(10); } catch (InterruptedException e) {}
            }
        });

        threadA.start();
        threadB.start();
    }
}
