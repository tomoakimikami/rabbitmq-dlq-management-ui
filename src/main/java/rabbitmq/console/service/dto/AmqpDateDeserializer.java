package rabbitmq.console.service.dto;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

/**
 * HTTP API用Date型デシリアライザ.
 *
 * @author Tomoaki Mikami
 */
public class AmqpDateDeserializer extends StdScalarDeserializer<Date> {

  /** serialVersionUID. */
  private static final long serialVersionUID = 1998298356309517260L;

  /**
   * デフォルトコンストラクタ.
   */
  public AmqpDateDeserializer() {
    super(AmqpDateDeserializer.class);
  }

  /**
   * {@inheritDoc}.
   */
  @Override
  public Date deserialize(JsonParser parser, DeserializationContext context)
      throws IOException {
    Long value = parser.getLongValue(); // エポックタイムだが、単位はミリ秒ではなく秒らしい
    Instant instant = Instant.ofEpochSecond(value);
    return Date.from(instant);
  }

}
