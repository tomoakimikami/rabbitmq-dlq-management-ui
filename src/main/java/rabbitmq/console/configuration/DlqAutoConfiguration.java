package rabbitmq.console.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Dead Letter Queue関連自動設定.
 *
 * @author Tomoaki Mikami
 */
@Configuration
@EnableConfigurationProperties(DlqProperties.class)
public class DlqAutoConfiguration {
  /**
   * プロパティ.
   */
  @SuppressWarnings("unused")
  @Autowired
  private DlqProperties dlqProperties;
}
