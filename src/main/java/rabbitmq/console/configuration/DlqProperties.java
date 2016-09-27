package rabbitmq.console.configuration;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Dead Letter Queue関連プロパティ.
 *
 * @author Tomoaki Mikami
 */
@ConfigurationProperties(prefix = "dlq.rabbitmq")
@Setter
@Getter
public class DlqProperties {
  /**
   * Dead Letter Queue名マップ(キー:DLQ名,値:バックアップキュー名).
   */
  private Map<String, String> deadLetterQueue;

  /**
   * 一覧取得最大件数.
   */
  private Integer maxCount = 10;
}
