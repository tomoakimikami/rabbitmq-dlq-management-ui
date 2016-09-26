package rabbitmq.console.component;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * リソースが見つからない場合の例外
 * @author Tomoaki Mikami
 *
 */
@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = -5126712894824395307L;
}
