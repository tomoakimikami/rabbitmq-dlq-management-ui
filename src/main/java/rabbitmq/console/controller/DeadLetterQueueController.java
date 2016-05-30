package rabbitmq.console.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import rabbitmq.console.service.QueueService;
import rabbitmq.console.service.dto.DeadLetteredMessage;
import rabbitmq.console.service.dto.DeadLetteredMessage.XDeath;

/**
 * Dead Letter Queueコントローラ.
 *
 * @author Tomoaki Mikami
 */
@Controller
public class DeadLetterQueueController {
  /**
   * キューサービス.
   */
  @Autowired
  private QueueService queueService;

  /**
   * Dead Letter Queueメッセージ一覧表示.
   *
   * @param model モデル
   * @return View指定キー
   */
  @RequestMapping(path = "/deadLetteredMessages", method = RequestMethod.GET)
  public String listDeadLetteredMessages(Model model) {
    // ログインユーザ
    model.addAttribute("username", queueService.resolveUsername());

    // 接続ホスト
    model.addAttribute("hostname", queueService.resolveHostname());

    // 接続ポート
    model.addAttribute("port", queueService.resolvePort());

    // vHost
    model.addAttribute("virtualHost", queueService.resolveVirtualHost());

    // Dead Letter キュー
    model.addAttribute("queuename", queueService.resolveDeadLetterQueue());

    // Dead Letter メッセージ
    List<DeadLetteredMessage> messages = queueService.listDeadLetteredMessages();
    model.addAttribute("messages", messages);

    // 現在時刻
    LocalDateTime now = LocalDateTime.now();
    ZonedDateTime zdt = now.atZone(ZoneId.systemDefault());
    Date lastUpdate = Date.from(zdt.toInstant());
    model.addAttribute("lastUpdate", lastUpdate);

    return "dlq/list";
  }

  /**
   * Dead Letter Queueメッセージ再登録.
   *
   * @param id メッセージid
   * @param attributes リダイレクト属性
   * @param model モデル
   * @return View指定キー
   */
  @RequestMapping(path = "/deadLetteredMessages/republish/{id}", method = RequestMethod.GET)
  public String republishMessage(@PathVariable String id, RedirectAttributes attributes,
      Model model) {
    // 対象メッセージを再登録
    DeadLetteredMessage message = queueService.findDeadLetteredMessage(id);
    queueService.republishMessage(message);
    // UnackedになったメッセージをReadyに戻しておく
    queueService.recoverAllUnackedMessages();

    // 再登録完了メッセージを渡す
    attributes.addFlashAttribute("republishedMessage", messageIdentity(message));
    return "redirect:/deadLetteredMessages";
  }

  /**
   * Dead Letter Queueメッセージ削除.
   *
   * @param id メッセージid
   * @param attributes リダイレクト属性
   * @param model モデル
   * @return View指定キー
   */
  @RequestMapping(path = "/deadLetteredMessages/delete/{id}", method = RequestMethod.GET)
  public String deleteMessage(@PathVariable String id, RedirectAttributes attributes, Model model) {
    // 対象メッセージを削除
    DeadLetteredMessage message = queueService.findDeadLetteredMessage(id);
    queueService.deleteMessage(message);
    // UnackedになったメッセージをReadyに戻しておく
    queueService.recoverAllUnackedMessages();

    // 削除完了メッセージを渡す
    attributes.addFlashAttribute("deletedMessage", messageIdentity(message));
    return "redirect:/deadLetteredMessages";
  }

  /**
   * Dead Letter Queueメッセージ削除およびバックアップ.
   *
   * @param id メッセージid
   * @param attributes リダイレクト属性
   * @param model モデル
   * @return View指定キー
   */
  @RequestMapping(path = "/deadLetteredMessages/deleteAndBackup/{id}", method = RequestMethod.GET)
  public String deleteAndBackupMessage(@PathVariable String id, RedirectAttributes attributes,
      Model model) {
    // 対象メッセージを削除およびバックアップキューへ退避
    DeadLetteredMessage message = queueService.findDeadLetteredMessage(id);
    queueService.deleteAndBackupMessage(message);
    // UnackedになったメッセージをReadyに戻しておく
    queueService.recoverAllUnackedMessages();

    // 削除完了メッセージを渡す
    attributes.addFlashAttribute("deletedMessage", messageIdentity(message));
    return "redirect:/deadLetteredMessages";
  }

  /**
   * Backup Queueメッセージ一覧表示.
   *
   * @param model モデル
   * @return View指定キー
   */
  @RequestMapping(path = "/backedUpMessages", method = RequestMethod.GET)
  public String listBackUpMessages(Model model) {
    // ログインユーザ
    model.addAttribute("username", queueService.resolveUsername());

    // 接続ホスト
    model.addAttribute("hostname", queueService.resolveHostname());

    // 接続ポート
    model.addAttribute("port", queueService.resolvePort());

    // vHost
    model.addAttribute("virtualHost", queueService.resolveVirtualHost());

    // バックアップメッセージ キュー
    model.addAttribute("queuename", queueService.resolveBackupQueue());

    // バックアップ メッセージ
    List<DeadLetteredMessage> messages = queueService.listBackedUpMessages();
    model.addAttribute("messages", messages);

    // 現在時刻
    LocalDateTime now = LocalDateTime.now();
    ZonedDateTime zdt = now.atZone(ZoneId.systemDefault());
    Date lastUpdate = Date.from(zdt.toInstant());
    model.addAttribute("lastUpdate", lastUpdate);

    return "backup/list";
  }

  /**
   * Backup Queueメッセージリストア.
   *
   * @param id メッセージid
   * @param attributes リダイレクト属性
   * @param model モデル
   * @return View指定キー
   */
  @RequestMapping(path = "/backedUpMessages/restore/{id}", method = RequestMethod.GET)
  public String restoreBackedUpMessage(@PathVariable String id, RedirectAttributes attributes,
      Model model) {
    // 対象メッセージを削除
    DeadLetteredMessage message = queueService.findBackedUpMessage(id);
    queueService.restoreBackedUpMessage(message);
    // UnackedになったメッセージをReadyに戻しておく
    queueService.recoverAllUnackedMessages();

    // リストア完了メッセージを渡す
    attributes.addFlashAttribute("restoreMessage", messageIdentity(message));
    return "redirect:/backedUpMessages";
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
}
