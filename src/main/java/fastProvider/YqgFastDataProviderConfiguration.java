package fastProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2024/2/3 20:14
 */
@Configuration
@EnableConfigurationProperties(YqgFastDataProviderProperties.class)
public class YqgFastDataProviderConfiguration {

  @Autowired
  private YqgFastDataProviderProperties providerProperties;

  @Bean
  public YqgFastDataProviderExecutor yqgFastDataProviderExecutor() {
    int maxThread = providerProperties.getMaxThread();
    if(maxThread <= 0) {
      maxThread = 2 * Runtime.getRuntime().availableProcessors() + 1;
    }
    return new YqgFastDataProviderExecutor(maxThread, providerProperties.getQueueSize());
  }
}
