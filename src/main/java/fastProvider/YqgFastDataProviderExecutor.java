package fastProvider;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2024/2/3 20:13
 */
@Slf4j
public class YqgFastDataProviderExecutor {
  private ThreadPoolExecutor commonPool;
  private int capacity;

  public YqgFastDataProviderExecutor(int maxThread, int queueSize) {
    this.commonPool = new ThreadPoolExecutor(maxThread, maxThread, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(queueSize + 1), (r, executor) -> { });
    this.capacity = queueSize;
  }

  public <T, R> YqgResultFuture<R> execute(List<T> condition, Function<List<T>, List<R>> function, int partition, Executor executor) {
    int size = condition.size() / partition;
    if (executor == null) {
      executor = commonPool;
      if (size > capacity) {
        partition = condition.size() / capacity;
        size = capacity;
      }
    }
    if (condition.size() % partition != 0) {
      size += 1;
    }
    CountDownLatch end = new CountDownLatch(size);
    CopyOnWriteArrayList<R> result = new CopyOnWriteArrayList<>();
    AtomicBoolean success = new AtomicBoolean(true);
    YqgResultFuture<R> future = new YqgResultFuture<>(end, result, success);
    Executor finalExecutor = executor;
    Lists.partition(condition, partition).forEach(
        list -> {
          try {
            finalExecutor.execute(()->{
              try {
                List<R> temp = function.apply(list);
                if(temp != null) {
                  result.addAll(temp);
                }
              } catch (Exception e) {
                log.error(e.getMessage(), e);
                success.set(false);
              } finally {
                end.countDown();
              }
            });
          } catch (Exception e) {
            log.warn(e.getMessage(), e);
            success.set(false);
            end.countDown();
          }
        }
    );
    return future;
  }



  public <T, R> YqgResultFuture<R> execute(List<T> condition, Function<List<T>, List<R>> function, Executor executor) {
    return execute(condition, function, 1, executor);
  }

  public <T, R> YqgResultFuture<R> execute(List<T> condition, Function<List<T>, List<R>> function, int partition) {
    return execute(condition, function, partition, null);
  }

  public <T, R> YqgResultFuture<R> execute(List<T> condition, Function<List<T>, List<R>> function) {
    return execute(condition, function, 1,null);
  }


}
