package com.mle.messaging.mpns

import play.api.http.HeaderNames._
import scala.xml.{NodeSeq, XML, Elem}
import scala.concurrent.Future
import java.io.StringWriter
import com.mle.util.Log
import com.ning.http.client.{Response => NingResponse}
import com.mle.http.AsyncHttp
import com.mle.util.Utils.executionContext

/**
 *
 * @author mle
 */
trait MPNS extends IMPNS with Log {
  //  val MessageID = "X-MessageID"
  // request headers
  val XNotificationClass = "X-NotificationClass"
  val NotificationType = "X-WindowsPhone-Target"
  // response headers
  val NotificationStatus = "X-NotificationStatus"
  val SubscriptionStatus = "X-SubscriptionStatus"
  val DeviceConnectionStatus = "X-DeviceConnectionStatus"

  val TILE = "tile"
  val TOAST = "toast"
  val RAW = "raw"
  val IMMEDIATE = "2"

  val TEXT_XML = "text/xml"

  private val toastHeaders = Map(
    CONTENT_TYPE -> TEXT_XML,
    NotificationType -> TOAST,
    XNotificationClass -> IMMEDIATE)

  def send(url: String, message: ToastMessage): Future[NingResponse] = send(url, toastXml(message))

  protected def send(url: String, xml: Elem): Future[NingResponse] =
    AsyncHttp.post(url, serialize(xml), toastHeaders)

  private def toastXml(message: ToastMessage): Elem =
    toastXml(message.text1, message.text2, message.deepLink, message.silent)

  /**
   *
   * @param text1
   * @param text2
   * @param deepLink The page to go to in app. For example: /page1.xaml?value1=1234&amp;value2=9876
   * @return
   */
  private def toastXml(text1: String, text2: String, deepLink: String, silent: Boolean): Elem = {
    val silenceElement = if (silent) <wp:Sound Silent="true"/> else NodeSeq.Empty
    // payloads must be on same line of xml, do not let formatting mess it up
    <wp:Notification xmlns:wp="WPNotification">
      <wp:Toast>
        <wp:Text1>
          {text1}
        </wp:Text1>
        <wp:Text2>
          {text2}
        </wp:Text2>
        <wp:Param>
          {deepLink}
        </wp:Param>{silenceElement}
      </wp:Toast>
    </wp:Notification>
  }

  /**
   * Serializes `elem` to a string, adding an xml declaration to the top. Encodes the payload
   * automatically as described in
   * http://msdn.microsoft.com/en-us/library/windowsphone/develop/hh202945(v=vs.105).aspx.
   *
   * @param elem xml
   * @return xml as a string
   */
  private def serialize(elem: Elem) = {
    val writer = new StringWriter
    // xmlDecl = true prepends this as the first line, as desired: <?xml version="1.0" encoding="utf-8"?>
    XML.write(writer, elem, "UTF-8", xmlDecl = true, doctype = null)
    val str = writer.toString
    //    log.info(str)
    str
  }

  //  def toast(text1: String, text2: String, deepLink: String, silent: Boolean): String => Future[NingResponse] =
  //    url => send(url, toastXml(text1, text2, deepLink, silent))
  //  def toast(message: ToastMessage): String => Future[NingResponse] =
  //    url => send(url, message)
  //  private def send(url: String, xml: Elem): Future[Response] =
  //    WS.url(url).withHeaders(toastHeaders: _*).post(serialize(xml))
}

trait IMPNS {
  def send(url: String, message: ToastMessage): Future[NingResponse]
}

case class ToastMessage(text1: String, text2: String, deepLink: String, silent: Boolean)

