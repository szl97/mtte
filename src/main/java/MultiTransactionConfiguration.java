import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2024/2/3 20:07
 */
@Configuration
@EnableConfigurationProperties(MultiTransactionProperties.class)
public class MultiTransactionConfiguration {
  @Autowired
  private MultiTransactionProperties transactionProperties;
  @Autowired
  private PlatformTransactionManager transactionManager;

  @Bean
  public MultiThreadTransactionExecutor multiThreadTransactionExecutor() {
    MultiThreadTransactionExecutor executor = new MultiThreadTransactionExecutor(
        transactionManager,
        transactionProperties.getMaxThreads(),
        transactionProperties.getMaxMainThreads(),
        transactionProperties.isFair(),
        transactionProperties.getMaxTransactions()
    );
    return executor;
  }
}
