import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2022/8/8 10:38 下午
 */
public class LayerLock {
  private CyclicBarrier barrier;
  private Semaphore semaphore;

  public LayerLock(int total, int concurrent) {
    this.barrier = new CyclicBarrier(total);
    this.semaphore = new Semaphore(concurrent);
  }

  public void await() throws BrokenBarrierException, InterruptedException {
    semaphore.release();
    barrier.await();
    semaphore.acquire();
  }

  public void release() {
    semaphore.release();
  }

  public void acquire() throws InterruptedException {
    semaphore.acquire();
  }

}
