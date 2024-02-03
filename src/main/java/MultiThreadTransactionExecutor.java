/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2024/2/3 19:49
 */

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @description: 当一个大任务分解为多线程执行，修改数据库时，保证事务同时提交和回滚
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2022/8/8 4:39 下午
 */
@Slf4j
public class MultiThreadTransactionExecutor {
  /**
   * 处于性能考虑，控制一次任务执行时的线程并行数，一次任务被执行时，线程的并行数为2*cpu
   */
  private int maxProcessors;
  /**
   * worker线程池的最大线程数，即实际执行任务的线程数量
   */
  private int maxThread;
  /**
   * 用于决定目前worker线程池的资源是否足以支撑一次任务的执行。
   * 信号量总数=maxThreads。当一个任务占用worker线程池的时候要先扣除所需线程数的信号量。扣除失败，才可占用线程。执行成功后返还。
   */
  private Semaphore semaphore;
  /**
   * 执行数据处理任务的线程池
   * 固定线程数
   */
  private Executor worker;
  /**
   * dataSource事务管理类
   */
  private PlatformTransactionManager transactionManager;
  /**
   * main线程池分配任务时是否采取公平方式。
   * 公平模式下，先加入的任务必定优先执行。当任务A资源不够时会阻塞，等待资源足够时，执行任务A
   * 非公平模式下，会根据资源量来进行分配，优先执行资源足够的任务。当任务A资源不够时，如果后面的任务B资源足够，会先执行任务B
   */
  private boolean fair;
  /**
   * main线程池用于分配任务，决定worker线程池下一步处理哪个任务，固定线程数，线程数代表在 worker线程池足够的情况下，最多多少任务被同时处理。
   * 例如 6个任务，执行每个任务都需要2条线程，worker的线程数为10。
   * 如果main线程数为3，此时3个任务可被同时处理，即 worker中6条线程在执行
   * 如果main线程数为5，此时5个任务可被同时处理，即 worker中10条线程在执行
   * 如果main线程数为10，此时5个任务可被同时处理，即 worker中10条线程在执行
   */
  private Executor main;

  public MultiThreadTransactionExecutor(PlatformTransactionManager transactionManager,
                                        int maxThread,
                                        int maxMainThread,
                                        boolean fair,
                                        int maxTransactions) {
    this.maxThread = maxThread;
    this.transactionManager = transactionManager;
    this.maxProcessors = Runtime.getRuntime().availableProcessors();
    this.worker = new ThreadPoolExecutor(maxThread, maxThread, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(maxThread), (r, executor) -> { });
    this.semaphore = new Semaphore(maxThread, fair);
    this.fair = fair;
    this.main = new ThreadPoolExecutor(maxMainThread, maxMainThread, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(maxTransactions), (r, executor) -> { });
  }

  /**
   * 执行任务
   * @param tasks 待处理数据
   * @param partition 每个线程的处理量
   * @param consumer 处理方法，consumer中MultiTransactionWorkerParam，除了持有待处理的数据，还持有一个LayerLock用于在方法中进行灵活的分层处理，
   *                 例如一次处理逻辑分为 stepA, stepB，且需要等待所有线程都执行完stepA后再执行stepB，那么在处理方法中按照如下方式使用：
   *                 {
   *                    ...stepA;
   *                    lock.await();
   *                    ...stepB;
   *                 }
   * @param failure 失败回调
   * @param success 成功回调
   * @param <T> 待处理数据的类型
   * 根据worker线程池的maxThread和设定的partition，计算出一次整体任务需要的线程数，如果一次整体任务所需要的线程超出了worker线程池的maxThread会产生死锁
   */
  public <T> void executeTransactionTask(List<T> tasks, int partition, Consumer<MultiTransactionWorkerParam<T>> consumer, Runnable failure, Runnable success) {
    try {
      if (tasks == null || tasks.size() == 0) {
        return;
      }
      int total = tasks.size();
      int thread = total / partition;
      if (total % partition != 0) {
        thread += 1;
      }
      if (thread > maxThread) {
        partition = total / maxThread + 1;
        thread = maxThread;
      }
      TransactionTask<T> task = new TransactionTask<>(partition, thread, tasks, consumer, failure, success);
      main.execute(() -> doMain(task));
    } catch (Exception exception) {
      log.error(exception.getMessage(), exception);
      runTask(failure);
    }
  }

  private  <T> void doMain(TransactionTask<T> task) {
    try {
      log.info("TransactionTask:{}", task.getThread());
      boolean success = semaphore.tryAcquire(task.getThread(), 1L, TimeUnit.SECONDS);
      if (success) {
        processTaskList(task);
      } else {
        if (fair) {
          semaphore.acquire(task.getThread());
          processTaskList(task);
        } else {
          main.execute(()->doMain(task));
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      runTask(task.getFailure());
    }
  }

  private <T> void processTaskList(TransactionTask<T> task) {
    try {
      LayerLock lock = new LayerLock(task.getThread(), maxProcessors * 2);
      CountDownLatch end = new CountDownLatch(task.getThread());
      AtomicBoolean error = new AtomicBoolean(false);
      DefaultTransactionDefinition def = new DefaultTransactionDefinition();
      Lists.partition(task.getList(), task.partition).stream().forEach(
          list -> worker.execute(() -> doWork(list, end, lock, def, task.getConsumer(), error))
      );
      end.await();
      if(error.get()) {
        runTask(task.getFailure());
      } else {
        runTask(task.getSuccess());
      }
    } catch (Exception e){
      log.error(e.getMessage(), e);
      task.getFailure().run();
    } finally {
      semaphore.release(task.getThread());
    }
  }

  private <T> void doWork(List<T> list, CountDownLatch end, LayerLock lock, DefaultTransactionDefinition def, Consumer<MultiTransactionWorkerParam<T>> consumer, AtomicBoolean error) {
    try {
      lock.acquire();
      TransactionStatus status = null;
      try {
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        status = transactionManager.getTransaction(def);
        consumer.accept(new MultiTransactionWorkerParam<>(list, lock));
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        error.set(true);
      } finally {
        end.countDown();
        lock.release();
      }
      end.await();
      if(error.get()) {
        if(status != null) {
          transactionManager.rollback(status);
        }
      } else {
        transactionManager.commit(status);
      }
    } catch (Exception e){
      error.set(true);
      end.countDown();
      log.error(e.getMessage(), e);
    }
  }

  private void runTask(Runnable runnable) {
    if(runnable != null) {
      runnable.run();
    }
  }

  @AllArgsConstructor
  @Getter
  class TransactionTask<T> {
    private int partition;
    private int thread;
    private List<T> list;
    private Consumer<MultiTransactionWorkerParam<T>> consumer;
    private Runnable failure;
    private Runnable success;
  }
}
