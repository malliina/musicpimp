package controllers

import com.mle.musicpimp.json.JsonMessages
import com.mle.play.ws.JsonWebSocket
import rx.lang.scala.Observable

import scala.concurrent.duration.DurationLong

/**
 * @author Michael
 */
trait PimpSocket extends JsonWebSocket with Secured {
  // prevents connections from being dropped after 30s of inactivity; i don't know how to modify that timeout
  val pinger = Observable.interval(20.seconds).subscribe(_ => broadcast(JsonMessages.Ping))

  override def welcomeMessage: Option[Message] = Some(com.mle.play.json.JsonMessages.welcome)
}
