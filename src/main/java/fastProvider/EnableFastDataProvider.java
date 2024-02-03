package fastProvider;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @description: some desc
 * @author: Stan Sai
 * @email: saizhuolin@gmail.com
 * @date: 2024/2/3 20:15
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({YqgFastDataProviderConfiguration.class})
public @interface EnableFastDataProvider {
}
