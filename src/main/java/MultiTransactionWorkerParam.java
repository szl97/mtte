import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2024/2/3 19:53
 */
@AllArgsConstructor
@Getter
public class MultiTransactionWorkerParam<T> {
  private List<T> list;
  private LayerLock lock;
}
