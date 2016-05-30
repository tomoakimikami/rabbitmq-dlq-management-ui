package rabbitmq.console.service;

import java.util.List;

import rabbitmq.console.service.dto.DeadLetteredMessage;

/**
 * RabbitMQのキュー情報を扱うサービス.
 *
 * @author Tomoaki Mikami
 */
public interface QueueService {
  /**
   * Dead Letter メッセージ一覧取得.
   *
   * @return Dead Letter メッセージ一覧
   */
  List<DeadLetteredMessage> listDeadLetteredMessages();

  /**
   * バックアップメッセージ一覧取得.
   *
   * @return バックアップメッセージ一覧
   */
  List<DeadLetteredMessage> listBackedUpMessages();

  /**
   * idに合致するDead Letterメッセージを取得.
   *
   * @param id メッセージID
   * @return idに合致するメッセージ
   */
  DeadLetteredMessage findDeadLetteredMessage(String id);

  /**
   * idに合致するバックアップメッセージを取得.
   *
   * @param id メッセージID
   * @return idに合致するメッセージ
   */
  DeadLetteredMessage findBackedUpMessage(String id);

  /**
   * DLQにあるメッセージを再登録.
   *
   * @param message メッセージ
   */
  void republishMessage(DeadLetteredMessage message);

  /**
   * DLQにあるメッセージを削除.
   *
   * @param message メッセージ
   */
  void deleteMessage(DeadLetteredMessage message);

  /**
   * DLQにあるメッセージを削除.
   *
   * @param message メッセージ
   */
  void deleteAndBackupMessage(DeadLetteredMessage message);

  /**
   * UnackedなメッセージをReadyにする.
   */
  void recoverAllUnackedMessages();

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
   * Dead Letter Queue名を導出する.
   *
   * @return 導出したDead Letter Queue名
   */
  String resolveDeadLetterQueue();

  /**
   * Backup Queue名を導出する.
   *
   * @return 導出したBackup Queue名
   */
  String resolveBackupQueue();

  /**
   * Backup QueueにあるメッセージをDead Letter Queueへ復元する.
   *
   * @param message 復元したいメッセージ
   */
  void restoreBackedUpMessage(DeadLetteredMessage message);
}
