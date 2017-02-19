package tests

import java.nio.file.{Files, Path}

import com.malliina.http.MultipartRequest
import com.malliina.util.Util
import com.malliina.util.Util._
import org.apache.commons.io.FileUtils
import org.scalatest.FunSuite

class UploadTests extends FunSuite {
  ignore("server plays uploaded track") {
    multiPartUpload("http://localhost:9000/playback/uploads")
  }

  def multiPartUpload(uri: String) {
    val file = TestUtils.makeTestMp3()
    using(new MultipartRequest(uri)) { req =>
      req.setAuth("admin", "test")
      req addFile file
      val response = req.execute()
      val statusCode = response.getStatusLine.getStatusCode
      assert(statusCode === 200)
    }
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
