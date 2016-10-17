package rabbitmq.console.component;

/**
 * basicPublis失敗時の例外
 * @author Tomoaki Mikami
 *
 */
public class BasicPublishException extends RuntimeException {
  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = -4663432643445942206L;

  /**
   * デフォルトコンストラクタ
   */
  public BasicPublishException() {
    throw new UnsupportedOperationException();
  }

  /**
   * コンストラクタ
   * @param cause エラーの要因となった例外
   */
  public BasicPublishException(Throwable cause) {
    super(cause);
  }
}
