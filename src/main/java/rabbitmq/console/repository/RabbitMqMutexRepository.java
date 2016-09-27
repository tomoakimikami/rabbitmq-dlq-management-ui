package rabbitmq.console.repository;

import rabbitmq.console.repository.entity.RabbitMqMutex;

/**
 * Mutexテーブルアクセス用リポジトリ.
 *
 * @author Tomoaki Mikami
 */
public interface RabbitMqMutexRepository {
  /**
   * 指定したミューテックスIdのエンティティが存在するかどうか
   * @param mutexId ミューテックスId
   * @return 存在すればtrue
   */
  boolean exists(Long mutexId);

  /**
   * ミューテックスを永続化する
   * @param mutex ミューテックス
   */
  void save(RabbitMqMutex mutex);

  /**
   * 指定したミューテックスIDのエンティティを削除する
   * @param mutexId ミューテックスID
   */
  void delete(Long mutexId);
}
