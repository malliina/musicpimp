package tests

import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.audio.Track
import com.malliina.play.{ContentRange, Streaming}
import com.malliina.storage.StorageLong
import com.malliina.ws.Streamer
import controllers.pimpcloud.{StreamReceiver, Uploads}
import org.scalatest.FunSuite
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.api.test.{FakeHeaders, FakeRequest}
import play.core.parsers.Multipart

import scala.concurrent.Future

class UploadTests extends FunSuite with BaseSuite {

  val as = ActorSystem("test")
  implicit val mat = ActorMaterializer()(as)
  implicit val ec = mat.executionContext
  val ctrl = new Uploads(Uploads.appMultipart(ec))

  val fileName = "01 Atb - Ecstacy (Intro Edit).mp3"
  val filePath = s"E:\\musik\\Elektroniskt\\A State Of Trance 600 Expedition\\CD 2 - ATB\\$fileName"
  val testFile = Paths get filePath

  ignore("can multipart") {
    // this test is broken
    val req = multipartRequest(testFile)
    val r = await(ctrl.up.apply(req))
    val bodyAsString = await(r.body.consumeData.map(_.utf8String))
    assert(r.header.status === 200)
  }

  ignore("can do it") {
    val (queue, source) = Streaming.sourceQueue[ByteString](mat)
    val parser = TestParsers.multiPartByteStreaming(bytes => {
      println(s"Got bytes $bytes")
      queue
        .offer(Option(bytes))
        .map(res => {
          println(res)
          ()
        })
        .recoverAll(onOfferError)
    }, Streamer.DefaultMaxUploadSize)(mat)
    val receiver = new StreamReceiver(mat)
    val streamer = new Streamer {
      override def snapshot = Nil

      override def exists(uuid: UUID) = false

      override def requestTrack(track: Track, range: ContentRange, req: RequestHeader) =
        Results.NotFound

      override def parser(uuid: UUID): Option[BodyParser[MultipartFormData[Long]]] = {
        println("Not here")
        None
      }

      override def remove(uuid: UUID, isCanceled: Boolean, wasSuccess: Boolean): Future[Boolean] =
        Future.successful(false)
    }
    val tempFile = Files.copy(testFile, Files.createTempFile("temp", null), StandardCopyOption.REPLACE_EXISTING)
    val size = Files.size(tempFile).bytes
    println(s"Sending $size in $tempFile")
    val action = receiver.receiveStream(parser, streamer, UUID.randomUUID())
    val request: Request[Either[MaxSizeExceeded, MultipartFormData[Long]]] =
      multipartRequest(tempFile).map[Either[MaxSizeExceeded, MultipartFormData[Long]]] { data =>
        analyze(data)
        Right(data)
      }
    val result = await(action.apply(request))
    println(await(result.body.consumeData.map(_.utf8String)))
    assert(result.header.status === 200)
    source.to(Sink.foreach(e => println(e.utf8String))).run()
  }

  def analyze(data: MultipartFormData[Long]): Unit = {
    val desc = data.files.map(part => part.filename + ": " + part.ref.bytes).mkString(", ")
    println(desc)
  }

  def onOfferError(t: Throwable) = {
    println(s"Offer failed for request", t)
  }

  def multipartRequest(file: Path): FakeRequest[MultipartFormData[Long]] = {
    val tempFile = TemporaryFile(file.toFile)
    val part = FilePart("file", file.getFileName.toString, None, tempFile)
    val files = Seq[FilePart[TemporaryFile]](part)
    val multiData = MultipartFormData(Map.empty, files, Nil)
    val partLengths = multiData.files.map(fp => fp.copy(ref = fp.ref.file.length()))
    val multiDataLengths = multiData.copy(files = partLengths)
    FakeRequest("POST", "/test", FakeHeaders(Seq("boundary" -> "123456789", "k" -> "v")), multiDataLengths)
  }

  /** Parse the content as multipart/form-data
    */
  def multipartFormData: BodyParser[MultipartFormData[TemporaryFile]] =
    multipartFormData(Multipart.handleFilePartAsTemporaryFile)

  val defaultLengths: Int = 512 * 1024 * 1024

  /** Parse the content as multipart/form-data
    *
    * @param filePartHandler Handles file parts.
    */
  def multipartFormData[A](filePartHandler: Multipart.FilePartHandler[A], maxLength: Long = defaultLengths): BodyParser[MultipartFormData[A]] = {
    BodyParser("multipartFormData") { request =>
      Multipart.multipartParser(defaultLengths, filePartHandler).apply(request)
    }
  }
}
