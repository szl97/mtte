import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2024/2/3 20:06
 */
@ConfigurationProperties(prefix = "multi.Transaction")
@Data
public class MultiTransactionProperties {
  private int maxThreads = 20;
  private int maxMainThreads = 3;
  private int maxTransactions = 10;
  private boolean fair = false;
}