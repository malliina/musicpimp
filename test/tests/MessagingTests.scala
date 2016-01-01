package tests

import com.malliina.musicpimp.messaging.adm.AdmClient
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

/**
 * @author Michael
 */
class MessagingTests extends FunSuite {
  test("can retrieve access token") {
    val tokenFuture = AdmClient.accessToken
    val token = Await.result(tokenFuture, 5.seconds)
    assert(token.expires_in === 3600.seconds)
  }
}
