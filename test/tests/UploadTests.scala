package tests

import java.nio.file.{Files, Path, Paths}

import com.malliina.http.MultipartRequest
import com.malliina.util.Util
import com.malliina.util.Util._
import org.apache.commons.io.FileUtils
import org.scalatest.FunSuite

class UploadTests extends FunSuite {
  val fileName = "mpthreetest.mp3"

  val tempFile = Paths get "nonexistent"

  test("server plays uploaded track") {
    multiPartUpload("http://localhost:9000/playback/uploads")
  }

  test("upload works") {
    //    org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    //      .asInstanceOf[ch.qos.logback.classic.Logger]
    //      .setLevel(ch.qos.logback.classic.Level.WARN)
    //
    //    val assertions = Future.sequence(List.fill(3)(uploadFuture))
    //    Await.result(assertions, 10.seconds)
  }

  def multiPartUpload(uri: String) {
    val file = ensureTestMp3Exists()
    using(new MultipartRequest(uri)) { req =>
      req.setAuth("admin", "test")
      req addFile file
      val response = req.execute()
      val statusCode = response.getStatusLine.getStatusCode
      assert(statusCode === 200)
    }
  }

  def ensureTestMp3Exists(): Path = {
    if (!Files.exists(tempFile)) {
      val dest = Files.createTempFile(null, null)
      val resourceURL = Util.resourceOpt(fileName)
      val url = resourceURL.getOrElse(throw new Exception(s"Resource not found: " + fileName))
      FileUtils.copyURLToFile(url, dest.toFile)
      if (!Files.exists(dest)) {
        throw new Exception(s"Unable to access $dest")
      }
      dest
    } else {
      tempFile
    }
  }
}
