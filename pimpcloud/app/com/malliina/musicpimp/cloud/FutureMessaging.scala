package com.malliina.musicpimp.cloud

import scala.concurrent.Future

/** A [[Future]]-based API for event-based apps. It is expected that once a message has been sent using `request`, within
  * a reasonable amount of time a response will be delivered to `complete`, which will complete the
  * [[Future]] returned by the initial call to `request`.
  *
  * Background: When you send a message, you may get [[Unit]] back, even though you might expect a response. This
  * construct attempts to overcome that limitation and provide you with a [[Future]] instead.
  *
  * @tparam T type of message and response
  */
trait FutureMessaging[T] {

  /** Sends a request.
    *
    * @param payload
    * @return
    */
  def send(payload: T): Unit

  /** Completes a request with `response`. It's assumed that we can find the matching `request` by parsing `response`.
    *
    * @param response response payload
    * @return true if a request was completed, false otherwise
    */
  def complete(response: T): Boolean
}
