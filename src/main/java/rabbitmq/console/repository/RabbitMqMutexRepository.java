package rabbitmq.console.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import rabbitmq.console.repository.entity.RabbitMqMutex;

/**
 * Mutexテーブルアクセス用リポジトリ.
 *
 * @author Tomoaki Mikami
 */
@Repository
public interface RabbitMqMutexRepository
    extends CrudRepository<RabbitMqMutex, Long>, JpaSpecificationExecutor<RabbitMqMutex> {
}
