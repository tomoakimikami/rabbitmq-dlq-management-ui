package rabbitmq.console.repository.entity;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import lombok.Data;

/**
 * Mutexエンティティ.
 *
 * @author Tomoaki Mikami
 */
@Entity(name = "RabbitMqMutex")
@Table(name = "RABBITMQ_MUTEX")
@Data
public class RabbitMqMutex implements Serializable {
  /** serialVersionUID. */
  private static final long serialVersionUID = 4645467854525140610L;

  /**
   * Mutex Id.
   */
  @Id
  @Column(name = "MUTEX", precision = 18, scale = 0)
  private Long mutex;

  /**
   * 作成時刻.
   */
  @Column(name = "CREATED_AT")
  @Temporal(value = TemporalType.TIMESTAMP)
  private Date createdAt;
}
