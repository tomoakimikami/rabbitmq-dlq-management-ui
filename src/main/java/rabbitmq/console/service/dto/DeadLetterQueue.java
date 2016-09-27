package rabbitmq.console.service.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Dead Letter Queue用DTO.
 *
 * @author Tomoaki Mikami
 */
@Getter
@Setter
@RequiredArgsConstructor
@ToString
public class DeadLetterQueue {
  /**
   * DLQ名
   */
  private String dlqName;

  /**
   * バックアップキュー名
   */
  private String backupQueueName;
}
