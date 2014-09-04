package com.mle.messaging.gcm

import com.mle.http.AsyncHttp
import com.mle.http.AsyncHttp._
import com.mle.util.Utils.executionContext
import com.ning.http.client.Response
import play.api.libs.json.Json

import scala.concurrent.Future

/**
 *
 * @author mle
 */
class GoogleMessaging(apiKey: String) {

  import com.mle.messaging.gcm.GoogleMessaging._

  def send(id: String, data: Map[String, String]): Future[Response] =
    send(GoogleMessage(Seq(id), data))

  def send(message: GoogleMessage): Future[Response] = {
    val body = Json toJson message
    AsyncHttp.postJson(POST_URL, body, Map(AUTHORIZATION -> s"key=$apiKey"))
  }
}

object GoogleMessaging {
  val POST_URL = "https://android.googleapis.com/gcm/send"
  val REGISTRATION_IDS = "registration_ids"
  val DATA = "data"
  val TIME_TO_LIVE = "time_to_live"
}

