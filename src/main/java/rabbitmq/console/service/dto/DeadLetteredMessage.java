package rabbitmq.console.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Dead Letter Message用DTO.
 *
 * @author Tomoaki Mikami
 */
@Getter
@Setter
@RequiredArgsConstructor
@ToString
@EqualsAndHashCode(exclude = {"redelivered", "messageCount"})
public class DeadLetteredMessage {
  /**
   * ペイロードのサイズ(バイト数).
   */
  @JsonProperty("payload_bytes")
  private Number payloadBytes = null;

  /**
   * このメッセージが再登録されたものか否か.
   */
  @JsonProperty("redelivered")
  private Boolean redelivered = null;

  /**
   * このメッセージ自体を登録した際のexchange名.
   */
  @JsonProperty("exchange")
  private String exchange = null;

  /**
   * このメッセージ自体を登録した際のルーティングキー.
   */
  @JsonProperty("routing_key")
  private String routingKey = null;

  /**
   * メッセージ数.
   */
  @JsonProperty("message_count")
  private Number messageCount = null;

  /**
   * ペイロード.
   */
  @JsonProperty("payload")
  private String payload = null;

  /**
   * ペイレードの元のエンコーディング.
   */
  @JsonProperty("payload_encoding")
  private String payloadEncoding = null;

  /**
   * 削除可能フラグ.
   */
  @JsonIgnore()
  private boolean deletable = false;

  /**
   * 再登録可能フラグ.
   */
  @JsonIgnore()
  private boolean republishable = false;

  /**
   * デッドレターキュー名
   */
  @JsonProperty("dlq_name")
  private String dlqName = null;

  /**
   * バックアップキュー名
   */
  @JsonProperty("backup_queue_name")
  private String backupQueueName = null;

  /**
   * メッセージ属性.
   */
  @JsonProperty("properties")
  private MessageProperties properties = new MessageProperties();

  /**
   * メッセージ属性.
   *
   * @author Tomoaki Mikami
   */
  @Data
  public static class MessageProperties {
    /**
     * メッセージID.
     */
    @JsonProperty("messageId")
    private String messageId = null;

    /**
     * メッセージヘッダ.
     */
    @JsonProperty("headers")
    private MessageHeader headers = new MessageHeader();
  }

  /**
   * メッセージヘッダ.
   *
   * @author Tomoaki Mikami
   */
  @Data
  public static class MessageHeader {
    /**
     * x-deathヘッダ情報リスト.
     */
    @JsonProperty("x-death")
    private List<XDeath> extraDeaths = new ArrayList<>();

    /**
     * x-message-mutexヘッダ情報.
     */
    @JsonProperty("x-message-mutex")
    private String extraMessageMutex = null;
  }

  /**
   * x-deathヘッダ情報.
   *
   * @author Tomoaki Mikami
   */
  @Data
  public static class XDeath {
    /**
     * Dead Letter Queue入りした理由.
     */
    @JsonProperty("reason")
    private String reason;

    /**
     * Dead Lettter Queue入りする前にいたキュー.
     */
    @JsonProperty("queue")
    private String queue;

    /**
     * Dead Letter Queue入りした時刻.
     */
    @JsonProperty("time")
    @JsonDeserialize(using = AmqpDateDeserializer.class)
    private Date time;

    /**
     * Dead Letter Queue入りした際に転送されたexchange.
     */
    @JsonProperty("exchange")
    private String exchange;

    /**
     * Dead Letter Queue入りした際に使用されたルーティングキーのリスト.
     */
    @JsonProperty("routing-keys")
    private List<String> routingKeys;
  }
}
