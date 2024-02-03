package fastProvider;

import lombok.AllArgsConstructor;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2024/2/3 20:11
 */
@AllArgsConstructor
public class YqgResultFuture<T> {
  private CountDownLatch end;
  private List<T> list;
  private AtomicBoolean success;

  public List<T> get() throws InterruptedException {
    end.await();
    if(success.get()) {
      return list;
    } else {
      return null;
    }
  }

  public List<T> get(long timeout, TimeUnit timeUnit) throws InterruptedException {
    boolean await = end.await(timeout, timeUnit);
    if(!await) {
      return null;
    }
    if(success.get()) {
      return list;
    } else {
      return null;
    }
  }

  public boolean isSuccess() throws InterruptedException {
    end.await();
    return success.get();
  }

  public boolean isSuccess(long timeout, TimeUnit timeUnit) throws InterruptedException {
    boolean await = end.await(timeout, timeUnit);
    return await && success.get();
  }
}

