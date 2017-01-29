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

  ignore("server plays uploaded track") {
    multiPartUpload("http://localhost:9000/playback/uploads")
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
