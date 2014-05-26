package tests

import org.scalatest.FunSuite
import com.mle.messaging.adm.AdmClient
import scala.concurrent.Await
import concurrent.duration.DurationInt

/**
 * @author Michael
 */
class MessagingTests extends FunSuite {
  test("can retrieve access token") {
    val tokenFuture = AdmClient.accessToken
    val token = Await.result(tokenFuture, 5 seconds)
    assert(token.expires_in === 3600.seconds)
  }
}
