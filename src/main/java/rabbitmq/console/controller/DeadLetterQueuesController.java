package rabbitmq.console.controller;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.Data;
import rabbitmq.console.component.ResourceNotFoundException;
import rabbitmq.console.service.QueueService;
import rabbitmq.console.service.dto.DeadLetterQueue;
import rabbitmq.console.service.dto.DeadLetteredMessage;
import rabbitmq.console.service.dto.DeadLetteredMessage.XDeath;

/**
 * Web用コントローラ
 *
 * @author Tomoaki Mikami
 *
 */
@Controller
@RequestMapping(path = "/deadLetterQueues")
public class DeadLetterQueuesController {
  /**
   * キューサービス.
   */
  @Autowired
  private QueueService queueService;

  /**
   * モデルに共通属性を追加設定
   *
   * @param model モデル
   */
  private void addCommonModelAttributes(Model model) {
    // ログインユーザ
    model.addAttribute("username", queueService.resolveUsername());

    // 接続ホスト
    model.addAttribute("hostname", queueService.resolveHostname());

    // 接続ポート
    model.addAttribute("port", queueService.resolvePort());

    // vHost
    model.addAttribute("virtualHost", queueService.resolveVirtualHost());

    // 現在時刻
    LocalDateTime now = LocalDateTime.now();
    ZonedDateTime zdt = now.atZone(ZoneId.systemDefault());
    Date lastUpdate = Date.from(zdt.toInstant());
    model.addAttribute("lastUpdate", lastUpdate);
  }

  /**
   * メッセージ特定用の文言を導出.
   *
   * @param message メッセージ
   * @return 文言
   */
  private String messageIdentity(DeadLetteredMessage message) {
    XDeath extraDeath = message.getProperties().getHeaders().getExtraDeaths().get(0);
    Date deadLetteredTime = extraDeath.getTime();
    String originalQueue = extraDeath.getQueue();
    return String.format("Dead Lettered Time: %s, Original Queue: %s", deadLetteredTime,
        originalQueue);
  }

  /**
   * Dead Letter Messageリストページへのリダイレクト用識別子を書式整形する
   * @param dlqName Dead Letter Queue名
   * @return 整形結果
   */
  private String formatRedirectToDlqMessageList(String dlqName) {
    return String.format("redirect:/deadLetterQueues/%s/messages", dlqName);
  }

  /**
   * Dead Letter Queue一覧表示.
   *
   * @param model モデル
   * @return View指定キー
   */
  @RequestMapping(method = RequestMethod.GET)
  public String listDeadLetterQueues(Model model) {
    // 共通属性
    addCommonModelAttributes(model);

    // キュー一覧取得
    List<DeadLetterQueue> queues = queueService.listDeadLetterQueues()//
        .entrySet()//
        .stream()//
        .map(entry -> {
          DeadLetterQueue queue = new DeadLetterQueue();
          queue.setDlqName(entry.getKey());
          queue.setBackupQueueName(entry.getValue());
          return queue;
        })//
        .collect(Collectors.toList());
    model.addAttribute("queues", queues);

    return "list";
  }

  /**
   * Dead Letter Queueメッセージ一覧表示
   *
   * @param dlqName Dead Letter Queue名
   * @param model モデル
   * @return View指定キー
   */
  @RequestMapping(path = "/{dlqName}/messages", method = RequestMethod.GET)
  public String listDeadLetteredMessages(@PathVariable String dlqName, Model model) {
    // 共通属性
    addCommonModelAttributes(model);

    // Dead Letter キュー
    model.addAttribute("dlqName", dlqName);

    // Dead Letter メッセージ
    List<DeadLetteredMessage> messages = queueService.listDeadLetteredMessages(dlqName);
    model.addAttribute("messages", messages);

    return "dlq/list";
  }

  /**
   * Dead Letter Queueメッセージ取得.
   *
   * @param dlqName Dead Letter Queue名
   * @param id メッセージid
   * @return メッセージ
   */
  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/{dlqName}/message/{id}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public MessageResponse findDeadLetteredMessage(@PathVariable String dlqName,
      @PathVariable String id) {
    DeadLetteredMessage message = queueService.findDeadLetteredMessage(dlqName, id);
    if (message == null) {
      throw new ResourceNotFoundException();
    }
    return convertToResponse(message);
  }

  /**
   * Dead Letter Queueメッセージ削除.
   *
   * @param dlqName Dead Letter Queue名
   * @param id メッセージid
   * @param attributes リダイレクト属性
   * @param model モデル
   * @return View指定キー
   */
  @RequestMapping(path = "/{dlqName}/delete/{id}", method = RequestMethod.GET)
  public String deleteMessage(@PathVariable String dlqName, @PathVariable String id,
      RedirectAttributes attributes, Model model) {
    // 対象メッセージを削除
    DeadLetteredMessage message = queueService.findDeadLetteredMessage(dlqName, id);
    queueService.deleteMessage(dlqName, message);
    // UnackedになったメッセージをReadyに戻しておく
    queueService.recoverAllUnackedMessages(dlqName);

    // 削除完了メッセージを渡す
    attributes.addFlashAttribute("deletedMessage", messageIdentity(message));
    return formatRedirectToDlqMessageList(dlqName);
  }

  /**
   * Dead Letter Queueメッセージ削除およびバックアップ.
   *
   * @param dlqName Dead Letter Queue名
   * @param id メッセージid
   * @param attributes リダイレクト属性
   * @param model モデル
   * @return View指定キー
   */
  @RequestMapping(path = "/{dlqName}/deleteAndBackup/{id}", method = RequestMethod.GET)
  public String deleteAndBackupMessage(@PathVariable String dlqName, @PathVariable String id,
      RedirectAttributes attributes, Model model) {
    // 対象メッセージを削除およびバックアップキューへ退避
    DeadLetteredMessage message = queueService.findDeadLetteredMessage(dlqName, id);
    String backupQueueName = queueService.resolveBackupQueueName(dlqName);
    queueService.deleteAndBackupMessage(dlqName, backupQueueName, message);
    // UnackedになったメッセージをReadyに戻しておく
    queueService.recoverAllUnackedMessages(dlqName);

    // 削除完了メッセージを渡す
    attributes.addFlashAttribute("deletedMessage", messageIdentity(message));
    return formatRedirectToDlqMessageList(dlqName);
  }

  /**
   * Dead Letter Queueメッセージ再登録.
   *
   * @param dlqName Dead Letter Queue名
   * @param id メッセージid
   * @param attributes リダイレクト属性
   * @param model モデル
   * @return View指定キー
   */
  @RequestMapping(path = "/{dlqName}/republish/{id}", method = RequestMethod.GET)
  public String republishMessage(@PathVariable String dlqName, @PathVariable String id,
      RedirectAttributes attributes, Model model) {
    // 対象メッセージを再登録
    DeadLetteredMessage message = queueService.findDeadLetteredMessage(dlqName, id);
    queueService.republishMessage(dlqName, message);
    // UnackedになったメッセージをReadyに戻しておく
    queueService.recoverAllUnackedMessages(dlqName);

    // 再登録完了メッセージを渡す
    attributes.addFlashAttribute("republishedMessage", messageIdentity(message));
    return formatRedirectToDlqMessageList(dlqName);
  }

  /**
   * Backup Queueメッセージ一覧表示
   *
   * @param dlqName dlq名
   * @param model モデル
   * @return View指定キー
   */
  @RequestMapping(path = "/{dlqName}/archivedMessages", method = RequestMethod.GET)
  public String listArchivedMessages(@PathVariable String dlqName, Model model) {
    // 共通属性
    addCommonModelAttributes(model);

    // Dead Letter Queueに対応するBackup Queue名を導出
    String backupQueueName = queueService.resolveBackupQueueName(dlqName);
    if (StringUtils.isEmpty(backupQueueName)) {
      throw new ResourceNotFoundException(); // 404を返す
    }

    // Dead Letter Queue名
    model.addAttribute("dlqName", dlqName);
    // Backup Queue名
    model.addAttribute("backupQueueName", backupQueueName);

    // Backup Queue メッセージ
    List<DeadLetteredMessage> messages = queueService.listBackedUpMessages(dlqName, backupQueueName);
    model.addAttribute("messages", messages);

    return "backup/list";
  }

  /**
   * Backup Queueメッセージ取得.
   *
   * @param dlqName Dead Letter Queue名
   * @param id メッセージid
   * @return メッセージ
   */
  @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE, path = "/{dlqName}/archivedMessage/{id}")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public MessageResponse findBackedUpMessage(@PathVariable String dlqName,
      @PathVariable String id) {
    // Dead Letter Queueに対応するBackup Queue名を導出
    String backupQueueName = queueService.resolveBackupQueueName(dlqName);
    if (StringUtils.isEmpty(backupQueueName)) {
      throw new ResourceNotFoundException(); // 404を返す
    }

    DeadLetteredMessage message = queueService.findBackedUpMessage(dlqName, backupQueueName, id);
    return convertToResponse(message);
  }

  /**
   * Backup Queueメッセージリストア.
   *
   * @param dlqName Dead Letter Queue名
   * @param id メッセージid
   * @param attributes リダイレクト属性
   * @param model モデル
   * @return View指定キー
   */
  @RequestMapping(path = "/{dlqName}/restore/{id}", method = RequestMethod.GET)
  public String restoreBackedUpMessage(@PathVariable String dlqName, @PathVariable String id,
      RedirectAttributes attributes, Model model) {
    // Dead Letter Queueに対応するBackup Queue名を導出
    String backupQueueName = queueService.resolveBackupQueueName(dlqName);
    if (StringUtils.isEmpty(backupQueueName)) {
      throw new ResourceNotFoundException(); // 404を返す
    }
    // 対象メッセージを削除
    DeadLetteredMessage message = queueService.findBackedUpMessage(dlqName, backupQueueName, id);
    queueService.restoreBackedUpMessage(dlqName, backupQueueName, message);
    // UnackedになったメッセージをReadyに戻しておく
    queueService.recoverAllUnackedMessages(backupQueueName);

    // リストア完了メッセージを渡す
    attributes.addFlashAttribute("restoreMessage", messageIdentity(message));
    return String.format("redirect:/deadLetterQueues/%s/archivedMessages", dlqName);
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
    return org.thymeleaf.util.StringUtils.abbreviate(payload, maxSize);
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
