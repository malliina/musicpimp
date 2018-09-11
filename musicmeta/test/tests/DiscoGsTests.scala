package tests

import java.io.Closeable

import com.malliina.concurrent.Execution.cached
import com.malliina.http.DiscoClient
import com.malliina.oauth.DiscoGsOAuthCredentials
import controllers.Covers
import org.scalatest.FunSuite

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class DiscoGsTests extends FunSuite {
  val uri = "http://api.discogs.com/image/R-5245462-1388609959-3809.jpeg"

  ignore("download cover") {
    val creds = DiscoGsOAuthCredentials("", "", "", "")
    using(new DiscoClient(creds, Covers.tempDir)) { client =>
      val result = client.downloadCover("Iron Maiden", "Powerslave")
        .map(p => s"Downloaded to $p")
        .recover { case t => s"Failure: $t" }
      val r = Await.result(result, 20.seconds)

      assert(r startsWith "Downloaded")
    }
  }

  def using[T <: Closeable, U](resource: T)(op: T => U): U =
    try {
      op(resource)
    } finally {
      resource.close()
    }
}
