package controllers

import java.io.IOException
import java.util.UUID

import akka.stream.Materializer
import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.storage.StorageLong
import com.malliina.ws.Streamer
import controllers.StreamReceiver.log
import play.api.Logger
import play.api.mvc._

import scala.concurrent.Future

class StreamReceiver(mat: Materializer) extends Controller {

  def receiveStream(parser: BodyParser[MultipartFormData[Long]],
                    transfers: Streamer,
                    requestId: UUID) = {
    val maxSize = transfers.maxUploadSize
    log debug s"Streaming at most $maxSize for request $requestId"
    val composedParser = recoveringParser(parse.maxLength(maxSize.toBytes, parser)(mat), transfers, requestId)
    Action(composedParser) { parsedRequest =>
      transfers.remove(requestId, shouldAbort = false, wasSuccess = true)
      parsedRequest.body.fold(
        tooMuch => {
          log error s"Max size of ${tooMuch.length} exceeded for request $requestId"
          EntityTooLarge
        },
        data => {
          val streamedSize = data.files.foldLeft(0L)((acc, part) => acc + part.ref).bytes
          val fileCount = data.files.size
          val fileDesc = if (fileCount > 1) "files" else "file"
          log info s"Streamed $streamedSize in $fileCount $fileDesc for request $requestId"
          Ok
        })
    }
  }

  /** This is not strictly necessary, but cleans up ugly and confusing stacktraces if
    * the client disconnects while an upload is in progress.
    */
  def recoveringParser[T](p: BodyParser[T], transfers: Streamer, requestId: UUID): BodyParser[T] =
    new BodyParser[T] {

      val clientClosedMessage = "An existing connection was forcibly closed by the remote host"
      val ioMessage = "Connection reset by peer"

      // this is redundant since the request is cleaned up elsewhere as well
      def cleanup() = transfers.remove(requestId, shouldAbort = false, wasSuccess = false)

      // if the client disconnects while an upload is in progress, a BodyParser throws
      // java.io.IOException: An existing connection was forcibly closed by the remote host
      override def apply(req: RequestHeader) = p(req) recoverWith {
        case t: IOException if Option(t.getMessage) exists (msg => Seq(clientClosedMessage, ioMessage).contains(msg)) =>
          log info s"Server cancelled upload for request $requestId"
          cleanup()
          Future.successful(Left(PartialContent))
        case t: Throwable =>
          cleanup()
          Future.failed(t)
      }
    }
}

object StreamReceiver {
  private val log = Logger(getClass)
}
