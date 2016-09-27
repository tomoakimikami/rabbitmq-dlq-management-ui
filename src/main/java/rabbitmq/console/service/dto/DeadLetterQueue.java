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
   * Dead Letter Queue名
   */
  private String dlqName;

  /**
   * Backup Queue名
   */
  private String backupQueueName;
}
