package rabbitmq.console.service;

import java.util.List;
import java.util.Map;

import rabbitmq.console.service.dto.DeadLetteredMessage;

/**
 * RabbitMQのキュー情報を扱うサービス.
 *
 * @author Tomoaki Mikami
 */
public interface QueueService {
  /**
   * Dead Letter Queue一覧取得.
   *
   * @return Dead Letter Queue一覧マップ(キー:Dead Letter Queue名,値:DL Backup Queue名)
   */
  Map<String, String> listDeadLetterQueues();

  /**
   * Dead Letter Queue名に対応するDL Backup Queue名を導出
   * @param dlqName Dead Letter Queue名
   * @return DL Backup Queue名。無ければnull
   */
  String resolveBackupQueueName(String dlqName);

  /**
   * Dead Letter メッセージ一覧取得.
   *
   * @param dlqName 一覧取得したいDLQ名
   * @return Dead Letter メッセージ一覧
   */
  List<DeadLetteredMessage> listDeadLetteredMessages(String dlqName);

  /**
   * バックアップメッセージ一覧取得.
   *
   * @param backupQueueName 一覧取得したいバックアップキュー名
   * @return バックアップメッセージ一覧
   */
  List<DeadLetteredMessage> listBackedUpMessages(String dlqName, String backupQueueName);

  /**
   * idに合致するDead Letterメッセージを取得.
   *
   * @param dlqName DLQ名
   * @param id メッセージID
   * @return idに合致するメッセージ
   */
  DeadLetteredMessage findDeadLetteredMessage(String dlqName, String id);

  /**
   * idに合致するバックアップメッセージを取得.
   *
   * @param backupQueueName バックアップキュー名
   * @param id メッセージID
   * @return idに合致するメッセージ
   */
  DeadLetteredMessage findBackedUpMessage(String dlqName, String backupQueueName, String id);

  /**
   * DLQにあるメッセージを再登録.
   *
   * @param dlqName DLQ名
   * @param message メッセージ
   */
  void republishMessage(String dlqName, DeadLetteredMessage message);

  /**
   * DLQにあるメッセージを削除.
   *
   * @param dlqName DLQ名
   * @param message メッセージ
   */
  void deleteMessage(String dlqName, DeadLetteredMessage message);

  /**
   * DLQにあるメッセージを削除してバックアップキューへ待避.
   *
   * @param dlqName DLQ名
   * @param backupQueueName バックアップキュー名
   * @param message メッセージ
   */
  void deleteAndBackupMessage(String dlqName, String backupQueueName, DeadLetteredMessage message);

  /**
   * UnackedなメッセージをReadyにする.
   *
   * @param dlqName DLQ名
   */
  void recoverAllUnackedMessages(String dlqName);

  /**
   * RabbitMQ接続ユーザ名を導出する.
   *
   * @return 導出したユーザ名
   */
  String resolveUsername();

  /**
   * RabbitMQ接続ホスト名を導出する.
   *
   * @return 導出したホスト名
   */
  String resolveHostname();

  /**
   * RabbitMQ接続ポートを導出する.
   *
   * @return 導出したポート
   */
  Integer resolvePort();

  /**
   * RabbitMQ仮想ホスト名を導出する.
   *
   * @return 導出した仮想ホスト名
   */
  String resolveVirtualHost();

  /**
   * Backup QueueにあるメッセージをDead Letter Queueへ復元する.
   *
   * @param dlqName DLQ名
   * @param backupQueueName バックアップキュー名
   * @param message 復元したいメッセージ
   */
  void restoreBackedUpMessage(String dlqName, String backupQueueName, DeadLetteredMessage message);
}
