package controllers.pimpcloud

import java.io.IOException

import akka.stream.Materializer
import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.musicpimp.models.RequestID
import com.malliina.storage.StorageLong
import com.malliina.ws.Streamer
import controllers.pimpcloud.StreamReceiver.log
import play.api.Logger
import play.api.mvc._

import scala.concurrent.Future

class StreamReceiver(mat: Materializer) extends Controller {

  def receiveStream(parser: BodyParser[MultipartFormData[Long]],
                    transfers: Streamer,
                    requestId: RequestID) = {
    val maxSize = transfers.maxUploadSize
    log info s"Streaming at most $maxSize for '$requestId'."
    //    val composedParser = recoveringParser(parse.maxLength(maxSize.toBytes, parser)(mat), transfers, requestId)
    val composedParser = recoveringParser(parser, transfers, requestId)
    Action(composedParser) { parsedRequest =>
      // Signals to the phone that the transfer is complete
      transfers.remove(requestId, shouldAbort = false, wasSuccess = true)
      val data = parsedRequest.body
      //      parsedRequest.body.fold(
      //        tooMuch => {
      //          log error s"Max size of ${tooMuch.length} exceeded for '$requestId'."
      //          EntityTooLarge
      //        },
      //        data => {
      //
      //        })
      val streamedSize = data.files.foldLeft(0L)((acc, part) => acc + part.ref).bytes
      val fileCount = data.files.size
      val fileDesc = if (fileCount > 1) "files" else "file"
      log info s"Streamed $streamedSize in $fileCount $fileDesc for '$requestId'."
      Ok
    }
  }

  /** This is not strictly necessary, but cleans up ugly and confusing stacktraces if
    * the client disconnects while an upload is in progress.
    */
  def recoveringParser[T](p: BodyParser[T], transfers: Streamer, requestId: RequestID): BodyParser[T] =
    new BodyParser[T] {
      val clientClosedMessage = "An existing connection was forcibly closed by the remote host"
      val ioMessage = "Connection reset by peer"
      val cancelMessages = Seq(clientClosedMessage, ioMessage)

      // Signals to the phone that the transfer is complete
      def cleanup() = transfers.remove(requestId, shouldAbort = false, wasSuccess = false)

      // If the client disconnects while an upload is in progress, a BodyParser throws
      // java.io.IOException: An existing connection was forcibly closed by the remote host
      override def apply(req: RequestHeader) = p(req) recoverWith {
        case t: IOException if Option(t.getMessage) exists (msg => cancelMessages.contains(msg)) =>
          log debug s"Server cancelled upload for '$requestId'."
          cleanup()
          Future.successful(Left(PartialContent))
        case t: Throwable =>
          log.error(s"Body parser failed for '$requestId'.", t)
          cleanup()
          Future.failed(t)
      }
    }
}

object StreamReceiver {
  private val log = Logger(getClass)
}
