package rabbitmq.console.configuration;

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
   * Dead Letter Queue名.
   */
  private String deadLetterQueue;

  /**
   * 削除バックアップ Queue名.
   */
  private String backupOnDeleteQueue;

  /**
   * 一覧取得最大件数.
   */
  private Integer maxCount = 10;
}
