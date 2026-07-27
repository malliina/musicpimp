package tests

import com.malliina.http.FullUrl
import com.malliina.http.OkClient.MultiPartFile
import com.malliina.http.io.HttpClientIO
import com.malliina.musicpimp.http.{HttpConstants, Rest}
import com.malliina.util.Util
import com.malliina.ws.HttpUtil
import org.apache.commons.io.FileUtils

import java.nio.file.{Files, Path}

class UploadTests extends munit.FunSuite:
  test("server plays uploaded track".ignore):
    multiPartUpload(FullUrl("http", "localhost:9000", "/playback/uploads"))

  val sslClient = HttpClientIO(Rest.okSslClient)

  def multiPartUpload(url: FullUrl): Unit =
    val file = TestUtils.makeTestMp3()
    val headers = Map(HttpConstants.AUTHORIZATION -> HttpUtil.authorizationValue("admin", "test"))
    val req = sslClient.multiPart(
      url,
      headers,
      files = Seq(MultiPartFile(Rest.audioMpeg, file))
    )
    req.map: res =>
      assertEquals(res.code, 200)

object TestUtils:
  def makeTestMp3() = TestUtils.resourceToFile("mpthreetest.mp3")

  def resourceToFile(resource: String, suffix: String = ".mp3"): Path =
    val dir = Files.createTempDirectory(null)
    val dest = Files.createTempFile(dir, null, suffix)
    val resourceURL = Util.resourceOpt(resource)
    val url = resourceURL.getOrElse(throw new Exception(s"Resource not found: $resource"))
    FileUtils.copyURLToFile(url, dest.toFile)
    if !Files.exists(dest) then throw new Exception(s"Unable to access $dest")
    dest
