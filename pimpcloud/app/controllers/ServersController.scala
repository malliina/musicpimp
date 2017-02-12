package controllers

import akka.util.ByteString
import com.malliina.concurrent.FutureOps
import com.malliina.pimpcloud.auth.CloudAuthentication
import com.malliina.play.Streaming
import com.malliina.play.streams.StreamParsers
import com.malliina.storage.StorageLong
import com.malliina.ws.Streamer
import controllers.ServersController.log
import play.api.Logger
import play.api.http.HttpEntity
import play.api.mvc._

class ServersController(cloudAuth: CloudAuthentication, auth: CloudAuth) extends StreamReceiver(auth.mat) {
  val mat = auth.mat
  implicit val ec = mat.executionContext

  def receiveUpload = serverAction(receive)

  def serverAction(f: ServerRequest => EssentialAction): EssentialAction =
    auth.loggedSecureActionAsync(cloudAuth.authServer)(f)

  def receive(server: ServerRequest): EssentialAction = {
    val requestID = server.request
    log debug s"Processing $requestID..."
    val transfers = server.socket.fileTransfers
    val parser = transfers parser requestID
    parser.fold[EssentialAction](Action(NotFound)) { parser =>
      receiveStream(parser, transfers, requestID)
    }
  }

  val (queue, source) = Streaming.sourceQueue[ByteString](mat)
  val parser = StreamParsers.multiPartByteStreaming(bytes => {
    queue
      .offer(Option(bytes))
      .map(res => ())
      .recoverAll(onOfferError)
  }, Streamer.DefaultMaxUploadSize)(mat)

  def registerListener = Action {
    log info "Registering..."
    Ok.sendEntity(HttpEntity.Streamed(source, None, None))
  }

  def receiveStream = receive(parser)

  def receive(parser: BodyParser[MultipartFormData[Long]]) = {
    Action(parser) { parsed =>
      val body = parsed.body
      val streamedSize = body.files.foldLeft(0L)((acc, part) => acc + part.ref).bytes
      val fileCount = body.files.size
      val fileDesc = if (fileCount > 1) "files" else "file"
      log info s"Streamed $streamedSize in $fileCount $fileDesc"
      Ok
    }
  }

  def onOfferError(t: Throwable) = {
    println(s"Offer failed for request", t)
  }
}

object ServersController {
  private val log = Logger(getClass)
}
