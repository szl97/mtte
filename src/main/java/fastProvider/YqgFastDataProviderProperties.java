package fastProvider;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2024/2/3 20:12
 */
@ConfigurationProperties(prefix = "fast.provider")
@Data
public class YqgFastDataProviderProperties {
  private int maxThread;
  private int queueSize = 1000;
}

