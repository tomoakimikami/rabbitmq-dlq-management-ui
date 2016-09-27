package rabbitmq.console.repository.entity;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * Mutexエンティティ.
 *
 * @author Tomoaki Mikami
 */
@Data
public class RabbitMqMutex implements Serializable {
  /** serialVersionUID. */
  private static final long serialVersionUID = 4645467854525140610L;

  /**
   * Mutex Id.
   */
  private Long mutex;

  /**
   * 作成時刻.
   */
  private Date createdAt;
}
