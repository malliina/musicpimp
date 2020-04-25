package tests

import java.nio.file.{Files, Path}

import com.malliina.http.FullUrl
import com.malliina.http.OkClient.MultiPartFile
import com.malliina.musicpimp.http.HttpConstants
import com.malliina.util.Util
import com.malliina.ws.HttpUtil
import controllers.musicpimp.Rest
import org.apache.commons.io.FileUtils

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class UploadTests extends munit.FunSuite {
  test("server plays uploaded track".ignore) {
    multiPartUpload(FullUrl("http", "localhost:9000", "/playback/uploads"))
  }

  def multiPartUpload(url: FullUrl): Unit = {
    val file = TestUtils.makeTestMp3()
    val headers = Map(HttpConstants.AUTHORIZATION -> HttpUtil.authorizationValue("admin", "test"))
    val req = Rest.sslClient.multiPart(
      url,
      headers,
      files = Seq(MultiPartFile(Rest.audioMpeg, file))
    )
    assert(Await.result(req, 10.seconds).code == 200)
  }
}

object TestUtils {
  def makeTestMp3() = TestUtils.resourceToFile("mpthreetest.mp3")

  def resourceToFile(resource: String, suffix: String = ".mp3"): Path = {
    val dir = Files.createTempDirectory(null)
    val dest = Files.createTempFile(dir, null, suffix)
    val resourceURL = Util.resourceOpt(resource)
    val url = resourceURL.getOrElse(throw new Exception(s"Resource not found: $resource"))
    FileUtils.copyURLToFile(url, dest.toFile)
    if (!Files.exists(dest)) {
      throw new Exception(s"Unable to access $dest")
    }
    dest
  }
}
