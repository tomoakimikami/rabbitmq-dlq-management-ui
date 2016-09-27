package rabbitmq.console.configuration;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ用設定情報.
 *
 * @author Tomoaki Mikami
 */
@Configuration
public class RabbitConfiguration {
  /**
   * RabiitMQプロパティ.
   */
  @SuppressWarnings("unused")
  @Autowired
  private RabbitProperties rabbitProperties;

  /**
   * RabbitMQオペレーション用テンプレートを取得.
   *
   * @param connectionFactory コネクションファクトリ
   * @return RabbitMQオペレーション用テンプレート
   */
  @Bean
  @Autowired
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    // Transactional
    rabbitTemplate.setChannelTransacted(true);
    // json
    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
    return rabbitTemplate;
  }
}
