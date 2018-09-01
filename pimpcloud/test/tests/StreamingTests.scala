package tests

import java.nio.file.{Files, Path, Paths}

import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink}
import akka.util.ByteString
import com.malliina.http.OkClient.MultiPartFile
import com.malliina.http.{FullUrl, OkClient}
import com.malliina.logstreams.client.HttpUtil
import com.malliina.pimpcloud.auth.FakeAuth
import com.malliina.play.Streaming
import com.malliina.security.SSLUtils
import com.malliina.concurrent.Execution.cached
import okhttp3.MediaType
import org.apache.commons.io.FileUtils
import play.api.http.HeaderNames

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future, Promise}

class StreamingTests extends PimpcloudSuite with BaseSuite {
  implicit val mat = app.materializer
  implicit val ec = mat.executionContext

  val fileName = "01 Atb - Ecstacy (Intro Edit).mp3"
  val file = s"E:\\musik\\Elektroniskt\\A State Of Trance 600 Expedition\\CD 2 - ATB\\$fileName"
  val filePath = Paths get file

  test("queue offer") {
    val (queue, source) = Streaming.sourceQueue[Int](mat, 0)
    val watchedSource = source.watchTermination() { (_, fut) =>
      fut.onComplete(res => println(s"Done $res"))
    }

    queue offer Option(1)
    val f = queue offer None
    intercept[IllegalStateException] {
      await(f)
    }
  }

  test("downstream (sink) cancellation terminates upstream (source)") {
    val message = "Completed"
    val p = Promise[String]()
    val (queue, source) = Streaming.sourceQueue[Int](mat, 0)
    val watchedSource = source.watchTermination() { (_, fut) =>
      fut.onComplete(_ => p.success(message))
      fut
    }
    queue offer Option(1)
    watchedSource.runWith(Sink.cancelled[Int])
    assert(await(p.future) === message)
  }

  test("buffers") {
    val (queue, source) = Streaming.sourceQueue[ByteString](mat, bufferSize = 0)
    // starts accepting events
    val handler = source.runWith(Sink.ignore)

    def send(s: String) = await(queue.offer(Option(ByteString(s))))

    send("abc")
    // closes queue: source terminates
    queue.offer(None)
    await(handler)
  }

  ignore("file to source") {
    val (queue, source) = Streaming.sourceQueue[ByteString](mat)
    val byteCalculator: Sink[ByteString, Future[Long]] = Sink.fold[Long, ByteString](0)((acc, bytes) => acc + bytes.length)
    val asyncSink = Flow[ByteString].mapAsync(1)(bytes => queue.offer(Option(bytes)).map(_ => bytes)).toMat(byteCalculator)(Keep.right)
    //    val bytesFuture = source.to(asyncSink).run()
    val bytes = FileIO.fromPath(filePath).runWith(asyncSink)
    Await.result(bytes, 10.seconds)
    println(bytes)
  }

  ignore("upload") {
    val client = OkClient.ssl(SSLUtils.trustAllSslContext().getSocketFactory, SSLUtils.trustAllTrustManager())
    // Register file listener
    val listenUri = FullUrl("http", "localhost:9000", "/testfile")
    val listenResponse = await(client.get(listenUri))
    assert(listenResponse.code === 200)
    Thread.sleep(200)
    // Upload file
    val uploadUri = FullUrl("http", "localhost:9000", "/testup")
    val request = client.multiPart(
      uploadUri,
      Map("request" -> FakeAuth.FakeUuid.toString),
      files = Seq(MultiPartFile(MediaType.parse("audio/mpeg"), filePath))
    )
    val response = await(request)
    assert(response.code === 200)
    client.close()
  }

  def multiPartUpload(uri: FullUrl, tempFile: Path): Unit = {
    val file = ensureTestMp3Exists(tempFile)
    val client = OkClient.default
    val request = client.multiPart(
      uri,
      Map(HeaderNames.AUTHORIZATION -> HttpUtil.authorizationValue("admin", "test")),
      files = Seq(MultiPartFile(MediaType.parse("audio/mpeg"), file))
    )
    val response = await(request)
    assert(response.code === 200)
    client.close()
  }

  def ensureTestMp3Exists(tempFile: Path): Path = {
    if (!Files.exists(tempFile)) {
      val dest = Files.createTempFile(null, null)
      val resourceURL = Option(getClass.getClassLoader).flatMap(cl => Option(cl.getResource(fileName)))
      val url = resourceURL.getOrElse(throw new Exception(s"Resource not found: $fileName"))
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
