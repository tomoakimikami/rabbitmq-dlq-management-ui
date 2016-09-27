package rabbitmq.console.repository.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import rabbitmq.console.repository.RabbitMqMutexRepository;
import rabbitmq.console.repository.entity.RabbitMqMutex;

/**
 * RabbitMqMutexRepositoryのJdbcTemplate実装
 *
 * @author Tomoaki Mikami
 *
 */
@Repository
public class RabbitMqMutexRepositoryImpl implements RabbitMqMutexRepository {
  /**
   * MUTEXカラム用プレースホルダー名
   */
  private static final String MUTEX_PLACE_HOLDER = "mutex";

  /**
   * CREATED_ATカラム用プレースホルダー名
   */
  private static final String CREATED_AT_PLACE_HOLDER = "createdAt";

  /**
   * JdbcTemplate
   */
  @Autowired
  public NamedParameterJdbcTemplate jdbcTemplate;

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean exists(Long mutexId) {
    String sql = String.format("SELECT COUNT(*) FROM RABBITMQ_MUTEX WHERE MUTEX = :%s",
        MUTEX_PLACE_HOLDER);
    SqlParameterSource paramSource = new MapSqlParameterSource()//
        .addValue(MUTEX_PLACE_HOLDER, mutexId);
    int result = jdbcTemplate.queryForObject(sql, paramSource, Integer.class);
    return result > 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void save(RabbitMqMutex mutex) {
    String sql = String.format("INSERT INTO RABBITMQ_MUTEX (MUTEX, CREATED_AT) VALUES  (:%s, :%s)",
        MUTEX_PLACE_HOLDER, CREATED_AT_PLACE_HOLDER);
    SqlParameterSource paramSource = new MapSqlParameterSource()//
        .addValue(MUTEX_PLACE_HOLDER, mutex.getMutex())//
        .addValue(CREATED_AT_PLACE_HOLDER, mutex.getCreatedAt());
    jdbcTemplate.update(sql, paramSource);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(Long mutexId) {
    String sql = String.format("DELETE FROM RABBITMQ_MUTEX WHERE MUTEX = :%s", MUTEX_PLACE_HOLDER);
    SqlParameterSource paramSource = new MapSqlParameterSource()//
        .addValue(MUTEX_PLACE_HOLDER, mutexId);
    jdbcTemplate.update(sql, paramSource);
  }

}
