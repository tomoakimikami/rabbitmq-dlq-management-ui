package rabbitmq.console.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.List;

import lombok.Data;
import rabbitmq.console.service.QueueService;
import rabbitmq.console.service.dto.DeadLetteredMessage;
import rabbitmq.console.service.dto.DeadLetteredMessage.XDeath;

/**
 * Dead Letter Restコントローラ.
 *
 * @author Tomoaki Mikami
 */
@RestController
public class DeadLetterRestController {
  /**
   * キューサービス.
   */
  @Autowired
  private QueueService queueService;

  /**
   * Dead Letter Queueメッセージ取得.
   *
   * @param id メッセージid
   * @return メッセージ
   */
  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
      path = "/deadLetteredMessages/{id}")
  @ResponseStatus(HttpStatus.OK)
  public MessageResponse findDeadLetteredMessage(@PathVariable String id) {
    DeadLetteredMessage message = queueService.findDeadLetteredMessage(id);
    MessageResponse response = convertToResponse(message);
    return response;
  }

  /**
   * Backup Queueメッセージ取得.
   *
   * @param id メッセージid
   * @return メッセージ
   */
  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
      path = "/backedUpMessages/{id}")
  @ResponseStatus(HttpStatus.OK)
  public MessageResponse findBackedUpMessage(@PathVariable String id) {
    DeadLetteredMessage message = queueService.findBackedUpMessage(id);
    MessageResponse response = convertToResponse(message);
    return response;
  }

  /**
   * Dead Lettered MessageをAPIレスポンス用DTOに詰め替える.
   *
   * @param message メッセージ
   * @return レスポンス用DTO
   */
  private MessageResponse convertToResponse(DeadLetteredMessage message) {
    if (message == null) {
      return null;
    }
    MessageResponse response = new MessageResponse();
    // messageId
    response.setMessageId(message.getProperties().getMessageId());
    // payload
    response.setPayload(abbreviatePayload(message.getPayload()));
    // deletable
    response.setDeletable(message.isDeletable());
    // republishable
    response.setRepublishable(message.isRepublishable());
    // mutex
    response.setMutexId(message.getProperties().getHeaders().getExtraMessageMutex());

    List<XDeath> extraDeaths = message.getProperties().getHeaders().getExtraDeaths();
    if (!extraDeaths.isEmpty()) {
      XDeath extraDeath = extraDeaths.get(0);
      // time
      if (extraDeath.getTime() != null) {
        response.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(extraDeath.getTime()));
      }
      // reason
      response.setReason(extraDeath.getReason());

    }
    return response;
  }

  /**
   * ペイロードは長くなる可能性があるので、ある程度のサイズで間引く.
   *
   * @param payload ペイロード
   * @return 間引いた結果の文字列
   */
  private String abbreviatePayload(String payload) {
    final int maxSize = 256;
    String result = StringUtils.abbreviate(payload, maxSize);
    return result;
  }

  /**
   * APIレスポンス用メッセージDTO.
   *
   * @author Tomoaki Mikami
   */
  @Data
  public static class MessageResponse {
    /**
     * メッセージID.
     */
    private String messageId;

    /**
     * Dead Lettered 時刻.
     */
    private String time;

    /**
     * ペイロード.
     */
    private String payload;

    /**
     * 理由.
     */
    private String reason;

    /**
     * ミューテックスID(二重送信制御されている場合のみ).
     */
    private String mutexId;

    /**
     * 削除可能かどうか.
     */
    private Boolean deletable;

    /**
     * 再登録可能かどうか.
     */
    private Boolean republishable;
  }
}
