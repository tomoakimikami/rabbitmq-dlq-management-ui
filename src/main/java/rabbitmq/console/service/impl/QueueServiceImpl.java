package rabbitmq.console.service.impl;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.LongString;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import rabbitmq.console.configuration.DlqProperties;
import rabbitmq.console.repository.RabbitMqMutexRepository;
import rabbitmq.console.repository.entity.RabbitMqMutex;
import rabbitmq.console.service.QueueService;
import rabbitmq.console.service.dto.DeadLetteredMessage;
import rabbitmq.console.service.dto.DeadLetteredMessage.MessageHeader;
import rabbitmq.console.service.dto.DeadLetteredMessage.XDeath;

/**
 * RabbitMQのキュー情報を扱うサービス.
 *
 * @author Tomoaki Mikami
 */
@Service
@Transactional(readOnly = true)
@Slf4j
public class QueueServiceImpl implements QueueService {
  /**
   * Dead Letter関連情報保持用ヘッダキー.
   */
  private static final String X_DEATH_KEY = "x-death";

  /**
   * Mutex ID保持用ヘッダキー.
   */
  private static final String X_MUTEX_KEY = "x-message-mutex";

  /**
   * Dead Letter Queue関連プロパティ.
   */
  @Autowired
  private DlqProperties dlqProperties;

  /**
   * RabbitMQ関連プロパティ.
   */
  @Autowired
  private RabbitProperties rabbitProperties;

  /**
   * Mutex ID保持テーブル用リポジトリ.
   */
  @Autowired
  private RabbitMqMutexRepository rabbitMqMutexRepository;

  /**
   * RabbitMQコネクションファクトリ.
   */
  @Autowired
  private ConnectionFactory connectionFactory;

  /**
   * RabbitMQテンプレート.
   */
  @Autowired
  private RabbitTemplate rabbitTemplate;

  /**
   * {@inheritDoc}.
   */
  @Override
  public void recoverAllUnackedMessages(String dlqName) {
    rabbitTemplate.setChannelTransacted(false);
    rabbitTemplate.execute(channel -> {
      boolean requeue = true;
      return channel.basicRecover(requeue);
    });
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public List<DeadLetteredMessage> listDeadLetteredMessages(String deadLetterQueueName) {
    return listMessages(deadLetterQueueName, null);
  }

  /**
   * {@inheritDoc}.
   * @param backupQueueName2
   */
  @Override
  public List<DeadLetteredMessage> listBackedUpMessages(String dlqName, String backupQueueName) {
    return listMessages(dlqName, backupQueueName);
  }

  /**
   * 指定されたキューにあるメッセージ一覧を取得する.
   *
   * @param queueName キュー名
   * @return メッセージ一覧
   */
  private List<DeadLetteredMessage> listMessages(String dlqName, String backupQueueName) {
    final String queueName = StringUtils.isEmpty(backupQueueName) ? dlqName : backupQueueName;
    rabbitTemplate.setChannelTransacted(true);
    List<DeadLetteredMessage> messageList = rabbitTemplate.execute(channel -> {
      List<DeadLetteredMessage> list = new ArrayList<>();
      final int prefetchCount = 1;
      channel.basicQos(prefetchCount);
      int maxCount = dlqProperties.getMaxCount();
      int count = 0;
      while (count < maxCount) {
        boolean autoAck = false;
        GetResponse response = channel.basicGet(queueName, autoAck);
        if (response == null) {
          break;
        }
        DeadLetteredMessage message = convertToMessage(response);
        if (message == null) { // 対象外メッセージはスキップ
          continue;
        }
        message.setDlqName(dlqName);
        message.setBackupQueueName(backupQueueName);
        log.debug(message.toString());
        list.add(message);
        // すべてNack(Unack)して、後でReadyに戻す。
        boolean multiple = false;
        boolean requeue = true;
        channel.basicNack(response.getEnvelope().getDeliveryTag(), multiple, requeue);
        count++;
      }
      return list;
    });
    return messageList;
  }

  /**
   * GetレスポンスをDeadLetterメッセージへ変換する.
   *
   * @param response Getレスポンス
   * @return DeadLetterメッセージ
   */
  private DeadLetteredMessage convertToMessage(GetResponse response) {
    Map<String, Object> extraDeathMap = extractXDeathMap(response);
    if (extraDeathMap.isEmpty()) {
      return null;
    }

    DeadLetteredMessage dlMessage = new DeadLetteredMessage();
    // メッセージ数
    dlMessage.setMessageCount(response.getMessageCount());
    // ペイロード
    final String encoding = "UTF-8";
    Charset charset = Charset.forName(encoding);
    byte[] body = response.getBody();
    dlMessage.setPayload(new String(body, charset));
    dlMessage.setPayloadBytes(body.length);
    dlMessage.setPayloadEncoding(encoding);

    Envelope envelope = response.getEnvelope();
    // ルーティングキー
    dlMessage.setRoutingKey(envelope.getRoutingKey());
    // エクスチェンジ
    dlMessage.setExchange(envelope.getExchange());
    // 再送信フラグ
    dlMessage.setRedelivered(envelope.isRedeliver());

    MessageHeader messageHeader = dlMessage.getProperties().getHeaders();
    List<XDeath> extraDeaths = messageHeader.getExtraDeaths();
    XDeath extraDeath = new XDeath();
    extraDeaths.add(extraDeath);
    LongString exchange = (LongString) extraDeathMap.get("exchange");
    extraDeath.setExchange(safetyToString(exchange));
    LongString queue = (LongString) extraDeathMap.get("queue");
    extraDeath.setQueue(safetyToString(queue));
    LongString reason = (LongString) extraDeathMap.get("reason");
    extraDeath.setReason(safetyToString(reason));
    Date time = (Date) extraDeathMap.get("time");
    extraDeath.setTime(time);
    // メッセージID
    dlMessage.getProperties().setMessageId(response.getProps().getMessageId());
    // mutex
    Map<String, Object> headers = response.getProps().getHeaders();
    if (headers != null) {
      LongString extraMessageMutex = (LongString) headers.get(X_MUTEX_KEY);
      messageHeader.setExtraMessageMutex(safetyToString(extraMessageMutex));
      if (StringUtils.isEmpty(messageHeader.getExtraMessageMutex())) { // ヘッダがない場合
        // 二重配信制御対象外なので削除も再登録もOK
        dlMessage.setRepublishable(true);
        dlMessage.setDeletable(true);
      } else { // ヘッダがある場合
        boolean exists = rabbitMqMutexRepository
            .exists(Long.valueOf(messageHeader.getExtraMessageMutex()));
        // RABBITMQ_MUTEXテーブルにMutex IDが存在すれば再登録OK
        dlMessage.setRepublishable(exists);
        // RABBITMQ_MUTEXテーブルにMutex IDが存在していたら削除不可
        dlMessage.setDeletable(!exists);
      }
    }
    return dlMessage;
  }

  /**
   * NPEを起こさずにオブジェクトの文字列表現を取得する.
   *
   * @param object 対象オブジェクト
   * @return オブジェクトの文字列表現。オブジェクトがNULLの場合はNULL
   */
  private String safetyToString(Object object) {
    if (object == null) {
      return null;
    }
    return object.toString();
  }

  /**
   * {@inheritDoc}.
   */
  @Transactional(readOnly = false)
  @Override
  public void republishMessage(String dlqName, DeadLetteredMessage message) {
    if (message == null) {
      return;
    }
    // 再登録処理
    rabbitTemplate.setChannelTransacted(true);
    rabbitTemplate.execute(channel -> {
      final int prefetchCount = 1;
      channel.basicQos(prefetchCount);
      while (true) {
        boolean autoAck = false;
        GetResponse response = channel.basicGet(dlqName, autoAck);
        if (response == null) {
          break;
        }
        // 再登録対象だけack。それ以外はNack(Unack)して、後でReadyに戻す。
        XDeath extraDeath = message.getProperties().getHeaders().getExtraDeaths().get(0);
        boolean multiple = false;
        if (isSameMessage(message, response)) {
          channel.basicAck(response.getEnvelope().getDeliveryTag(), multiple);
          log.info(
              String.format("Message Acked: %s, %s", extraDeath.getTime(), extraDeath.getQueue()));
          // 再登録
          republishDeadLetteredMessage(dlqName, channel,
              response);
        } else {
          boolean requeue = true;
          channel.basicNack(response.getEnvelope().getDeliveryTag(), multiple, requeue);
          log.info(String.format("Message Unacked: %s, %s", extraDeath.getTime(),
              extraDeath.getQueue()));
        }
      }
      return null;
    });
  }

  /**
   * 受信したDead Letterメッセージを元のキューへ再登録する.
   *
   * @param dlqName DLQ名
   * @param channel チャネル
   * @param response 受信メッセージ
   * @throws IOException IOエラー発生時
   */
  private void republishDeadLetteredMessage(String dlqName, Channel channel,
      GetResponse response) throws IOException {
    Map<String, Object> extraDeathMap = extractXDeathMap(response);
    if (extraDeathMap.isEmpty()) {
      return;
    }
    BasicProperties props = response.getProps();
    byte[] body = response.getBody();
    LongString exchange = (LongString) extraDeathMap.get("exchange");
    @SuppressWarnings("unchecked")
    List<LongString> routingKeys = (List<LongString>) extraDeathMap.get("routing-keys");
    LongString routingKey = routingKeys.get(0);
    boolean mandatory = false;
    boolean immediate = false;
    // Dead Letter関連情報をクリア
    Map<String, Object> headerMap = props.getHeaders();
    headerMap.remove(X_DEATH_KEY);
    channel.basicPublish(safetyToString(exchange), safetyToString(routingKey), mandatory, immediate,
        props, body);
    log.info(String.format("Republished. Exchange:%s,Routing-Key:%s", safetyToString(exchange),
        safetyToString(routingKey)));
  }

  /**
   * レスポンスからDead Letter関連情報を抽出.
   *
   * @param response レスポンス
   * @return x-deathヘッダ情報
   */
  private Map<String, Object> extractXDeathMap(GetResponse response) {
    Map<String, Object> extraDeathMap = new HashMap<String, Object>();
    Map<String, Object> headers = response.getProps().getHeaders();
    if (headers != null) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> extraDeathList = (List<Map<String, Object>>) headers
          .getOrDefault(X_DEATH_KEY, new ArrayList<Map<String, Object>>());
      if (!extraDeathList.isEmpty()) {
        extraDeathMap = extraDeathList.get(0);
      }
    }
    return extraDeathMap;
  }

  /**
   * 処理対象メッセージとConnection経由で取得したレスポンスのメッセージが同一かどうかを判定.
   *
   * @param deadLetteredMessage 処理対象メッセージ
   * @param response レスポンス
   * @return 同一であればtrue
   */
  private boolean isSameMessage(DeadLetteredMessage deadLetteredMessage, GetResponse response) {
    // メッセージIDを再優先で比較
    String responseMessageId = response.getProps().getMessageId();
    String messageId = deadLetteredMessage.getProperties().getMessageId();
    if (!StringUtils.isEmpty(messageId) && !StringUtils.isEmpty(responseMessageId)) {
      if (messageId.equals(responseMessageId)) {
        return true;
      }
      return false;
    }
    MessageHeader messageHeader = deadLetteredMessage.getProperties().getHeaders();
    String mutex = messageHeader.getExtraMessageMutex();
    XDeath extraDeath = messageHeader.getExtraDeaths().get(0);
    String originalQueue = extraDeath.getQueue();
    Date deadLeteredTime = extraDeath.getTime();

    Map<String, Object> extraDeathMap = extractXDeathMap(response);
    if (!extraDeathMap.isEmpty()) {
      // タイムスタンプ
      Date targetTime = (Date) extraDeathMap.get("time");
      // 元キュー
      LongString targetQueue = (LongString) extraDeathMap.get("queue");
      // mutex id
      LongString mutexKey = (LongString) response.getProps().getHeaders().get(X_MUTEX_KEY);

      if (targetTime.equals(deadLeteredTime) && originalQueue.equals(safetyToString(targetQueue))
          && mutex.equals(safetyToString(mutexKey))) {
        return true;
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}.
   */
  @Transactional(readOnly = false)
  @Override
  public void deleteMessage(String dlqName, DeadLetteredMessage message) {
    if (message == null) {
      return;
    }

    // キューから削除
    rabbitTemplate.setChannelTransacted(true);
    rabbitTemplate.execute(channel -> {
      final int prefetchCount = 1;
      channel.basicQos(prefetchCount);
      while (true) {
        boolean autoAck = false;
        GetResponse response = channel.basicGet(dlqName, autoAck);
        if (response == null) {
          break;
        }
        // 削除対象だけack。それ以外はNack(Unack)して、後でReadyに戻す。
        XDeath extraDeath = message.getProperties().getHeaders().getExtraDeaths().get(0);
        boolean multiple = false;
        if (isSameMessage(message, response)) {
          channel.basicAck(response.getEnvelope().getDeliveryTag(), multiple);
          log.info(
              String.format("Message Acked: %s, %s", extraDeath.getTime(), extraDeath.getQueue()));
        } else {
          boolean requeue = true;
          channel.basicNack(response.getEnvelope().getDeliveryTag(), multiple, requeue);
          log.info(String.format("Message Unacked: %s, %s", extraDeath.getTime(),
              extraDeath.getQueue()));
        }
      }
      return null;
    });

    // ミューテックス削除
    deleteMutex(message);
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public void deleteAndBackupMessage(String dlqName, String backupQueueName, DeadLetteredMessage message) {
    if (message == null) {
      return;
    }

    // Dead Letterキューから削除
    rabbitTemplate.setChannelTransacted(true);
    rabbitTemplate.execute(channel -> {
      final int prefetchCount = 1;
      channel.basicQos(prefetchCount);
      while (true) {
        boolean autoAck = false;
        GetResponse response = channel.basicGet(dlqName, autoAck);
        if (response == null) {
          break;
        }
        // 削除対象だけack。それ以外はNack(Unack)して、後でReadyに戻す。
        XDeath extraDeath = message.getProperties().getHeaders().getExtraDeaths().get(0);
        boolean multiple = false;
        if (isSameMessage(message, response)) {
          // 削除対象メッセージをバックアップキューへ登録
          backupDeadLetteredMessage(dlqName, backupQueueName, channel, response);
          channel.basicAck(response.getEnvelope().getDeliveryTag(), multiple);
          log.info(
              String.format("Message Acked: %s, %s", extraDeath.getTime(), extraDeath.getQueue()));
        } else {
          boolean requeue = true;
          channel.basicNack(response.getEnvelope().getDeliveryTag(), multiple, requeue);
          log.info(String.format("Message Unacked: %s, %s", extraDeath.getTime(),
              extraDeath.getQueue()));
        }
      }
      return null;
    });

    // ミューテックス削除
    deleteMutex(message);
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public void restoreBackedUpMessage(String dlqName, String backupQueueName, DeadLetteredMessage message) {
    if (message == null) {
      return;
    }

    // バックアップキューから削除
    rabbitTemplate.setChannelTransacted(true);
    rabbitTemplate.execute(channel -> {
      final int prefetchCount = 1;
      channel.basicQos(prefetchCount);
      while (true) {
        boolean autoAck = false;
        GetResponse response = channel.basicGet(backupQueueName, autoAck);
        if (response == null) {
          break;
        }
        // 削除対象だけack。それ以外はNack(Unack)して、後でReadyに戻す。
        XDeath extraDeath = message.getProperties().getHeaders().getExtraDeaths().get(0);
        boolean multiple = false;
        if (isSameMessage(message, response)) {
          // 復元対象メッセージをDead Letterキューへ登録
          restoreBackedUpMessage(dlqName, backupQueueName, channel, response);
          channel.basicAck(response.getEnvelope().getDeliveryTag(), multiple);
          log.info(
              String.format("Message Acked: %s, %s", extraDeath.getTime(), extraDeath.getQueue()));
        } else {
          boolean requeue = true;
          channel.basicNack(response.getEnvelope().getDeliveryTag(), multiple, requeue);
          log.info(String.format("Message Unacked: %s, %s", extraDeath.getTime(),
              extraDeath.getQueue()));
        }
      }
      return null;
    });

    // ミューテックス復活
    saveMutex(message);
  }

  /**
   * バックアップメッセージをDead Letterキューへ再登録する.
   *
   * @param channel チャネル
   * @param response メッセージ
   * @throws IOException IOエラー発生時
   */
  private void restoreBackedUpMessage(String dlqName, String backupQueueName, Channel channel, GetResponse response) throws IOException {
    BasicProperties props = response.getProps();
    byte[] body = response.getBody();
    String exchange = "";
    String routingKey = dlqName;
    boolean mandatory = false;
    boolean immediate = false;
    channel.basicPublish(exchange, routingKey, mandatory, immediate, props, body);
    log.info(String.format("Restored. Exchange:%s,Routing-Key:%s", exchange, routingKey));
  }

  /**
   * 受信したDead Letterメッセージをバックアップキューへ再登録する.
   *
   * @param channel チャネル
   * @param response 受信メッセージ
   * @throws IOException IOエラー発生時
   */
  private void backupDeadLetteredMessage(String dlqName, String backupQueueName, Channel channel, GetResponse response) throws IOException {
    BasicProperties props = response.getProps();
    byte[] body = response.getBody();
    String exchange = "";
    String routingKey = backupQueueName;
    boolean mandatory = false;
    boolean immediate = false;
    channel.basicPublish(exchange, routingKey, mandatory, immediate, props, body);
    log.info(String.format("Backuped. Exchange:%s,Routing-Key:%s", exchange, routingKey));
  }

  /**
   * ミューテックス登録.
   *
   * @param message Dead Letter メッセージ
   */
  private void saveMutex(DeadLetteredMessage message) {
    String extraMessageMutex = message.getProperties().getHeaders().getExtraMessageMutex();
    if (!StringUtils.isEmpty(extraMessageMutex)) {
      Long id = Long.valueOf(extraMessageMutex);
      if (!rabbitMqMutexRepository.exists(id)) {
        RabbitMqMutex mutex = new RabbitMqMutex();
        mutex.setMutex(id);
        mutex.setCreatedAt(Calendar.getInstance().getTime());
        rabbitMqMutexRepository.save(mutex);
      }
    }
  }

  /**
   * ミューテックス削除.
   *
   * @param message Dead Letter メッセージ
   */
  private void deleteMutex(DeadLetteredMessage message) {
    String extraMessageMutex = message.getProperties().getHeaders().getExtraMessageMutex();
    if (!StringUtils.isEmpty(extraMessageMutex)) {
      Long id = Long.valueOf(extraMessageMutex);
      if (rabbitMqMutexRepository.exists(id)) {
        rabbitMqMutexRepository.delete(id);
      }
    }
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public DeadLetteredMessage findDeadLetteredMessage(String dlqName, String id) {
    List<DeadLetteredMessage> messages = listDeadLetteredMessages(dlqName);
    return messages.stream()//
        .filter(message -> id.equals(message.getProperties().getMessageId()))//
        .findFirst()//
        .orElse(null);
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public DeadLetteredMessage findBackedUpMessage(String dlqName, String backupQueueName, String id) {
    List<DeadLetteredMessage> messages = listBackedUpMessages(dlqName, backupQueueName);
    return messages.stream()//
        .filter(message -> id.equals(message.getProperties().getMessageId()))//
        .findFirst()//
        .orElse(null);
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public String resolveUsername() {
    return rabbitProperties.getUsername();
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public String resolveHostname() {
    return connectionFactory.getHost();
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public Integer resolvePort() {
    return connectionFactory.getPort();
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public String resolveVirtualHost() {
    return connectionFactory.getVirtualHost();
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public Map<String, String> listDeadLetterQueues() {
    return dlqProperties.getDeadLetterQueue();
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public String resolveBackupQueueName(String dlqName) {
    return listDeadLetterQueues().get(dlqName);
  }
}
