package controllers.musicpimp

import java.io._
import java.net.UnknownHostException
import java.nio.file._
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit

import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.malliina.audio.meta.{SongMeta, SongTags, StreamSource}
import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.file.FileUtilities
import com.malliina.http.OkClient.MultiPartFile
import com.malliina.http.{HttpResponse, OkClient}
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.beam.BeamCommand
import com.malliina.musicpimp.http.PimpContentController
import com.malliina.musicpimp.http.PimpContentController.default
import com.malliina.musicpimp.json.{JsonMessages, JsonStrings}
import com.malliina.musicpimp.library.{Library, LocalTrack, MusicLibrary}
import com.malliina.musicpimp.models._
import com.malliina.play.controllers.Caching.{NoCache, NoCacheOk}
import com.malliina.play.http.{AuthedRequest, CookiedRequest, FullUrls, OneFileUploadRequest}
import com.malliina.play.streams.{StreamParsers, Streams}
import com.malliina.security.SSLUtils
import com.malliina.storage.{StorageInt, StorageLong}
import com.malliina.util.Utils
import com.malliina.values.{ErrorMessage, UnixPath, Username}
import com.malliina.ws.HttpUtil
import controllers.musicpimp.Rest.log
import javax.net.ssl.X509TrustManager
import okhttp3.MediaType
import play.api.Logger
import play.api.http.{HeaderNames, HttpErrorHandler}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.Accumulator
import play.api.libs.{Files => PlayFiles}
import play.api.mvc._

import scala.concurrent.Future
import scala.util.Try

class Rest(lib: MusicLibrary,
           auth: AuthDeps,
           handler: PlaybackMessageHandler,
           statsPlayer: StatsPlayer,
           errorHandler: HttpErrorHandler)
  extends Secured(auth) {

  def ping = Action(NoCache(Ok))

  def pingAuth = pimpAction(_ => NoCacheOk(JsonMessages.version))

  /** Handles server playback commands POSTed as JSON.
    */
  def playback = jsonAckAction(handler.onJson)

  /** Alias for `playback`. Should be deprecated.
    */
  def playlist = playback

  def playUploadedFile = UploadedSongAction { track =>
    MusicPlayer.setPlaylistAndPlay(track)
  }

  /** Adds the uploaded track to the server playlist.
    */
  def addUpload = UploadedSongAction { track =>
    MusicPlayer.playlist.add(track)
  }

  /** Starts playback of the track in the request.
    *
    * First authenticates, then reads the Track header, then attempts to find the track ID from
    * local storage. If found, starts playback of the local track, otherwise, starts playback of
    * the streamed track available in the body of the request.
    *
    * Returns Unauthorized if authentication fails (as usual), and BadRequest with JSON if the
    * Track header is faulty. Otherwise returns 200 OK.
    *
    * TODO: See and fix https://github.com/playframework/playframework/issues/1842
    */
  def streamedPlayback = authenticated { (req: AuthedRequest) =>
    com.malliina.musicpimp.audio.Track
    EssentialAction { requestHeader =>
      val headerValue = requestHeader.headers.get(JsonStrings.TrackHeader)
        .map(Json.parse(_).validate[Track](Track.jsonFormat))
      val metaOrError = headerValue.map(jsonResult => jsonResult.fold(
        invalid => Left(loggedJson(s"Invalid JSON: $invalid")),
        valid => Right(valid))
      ).getOrElse(Left(loggedJson("No Track header is defined.")))
      val authAction = metaOrError.fold(
        error => pimpAction(BadRequest(error)),
        meta => {
          statsPlayer.updateUser(req.user)
          EssentialAction { rh =>
            val a = localPlaybackAction(meta.id).map(_.getOrElse(streamingAction(meta))).map(_.apply(rh))
            Accumulator.flatten(a)(mat)
          }
        }
      )
      authAction(requestHeader)
    }
  }

  /** Beams the local track according to the [[BeamCommand]] in the request body.
    *
    * TODO: if no root folder for the track is found this shit explodes, fix and return an erroneous HTTP response instead
    */
  def stream = pimpParsedActionAsync(parsers.json) { req =>
    Json.fromJson[BeamCommand](req.body).fold(
      invalid = _ => fut(BadRequest(JsonMessages.invalidJson)),
      valid = cmd => {
        val (response, duration) = Utils.timed(Rest.beam(cmd, lib))
        response.map { e =>
          e.fold(
            errorMsg => {
              badRequest(errorMsg.message)
            },
            httpResponse => {
              // relays MusicBeamer's response to the client
              val statusCode = httpResponse.code
              log info s"Completed track upload in: $duration, relaying response: $statusCode"
              val result = new Results.Status(statusCode)
              if (statusCode >= 200 && statusCode < 300) result(JsonMessages.thanks)
              else result
            })
        }.recover {
          case uhe: UnknownHostException =>
            notFound(s"Unable to find MusicBeamer endpoint. ${uhe.getMessage}")
          case e: Exception =>
            val msg = "Stream failure."
            log.error(msg, e)
            serverError(msg)
        }
      }
    )
  }

  def status = pimpAction { req =>
    val host = FullUrls.hostOnly(req)
    PimpContentController.default.pimpResponse(req)(
      html = Results.NoContent,
      json17 = Json.toJson(MusicPlayer.status17(host)),
      latest = Json.toJson(MusicPlayer.status(host))
    )
  }

  private def localPlaybackAction(id: TrackID): Future[Option[EssentialAction]] =
    lib.meta(id).map { maybeTrack =>
      maybeTrack.map { track =>

        /** The MusicPlayer is intentionally modified outside of the PimpAction block. Here's why this is correct:
          *
          * The request has already been authenticated at this point because this method is called from within an
          * Authenticated block only, see `streamedPlayback`. The following authentication made by PimpAction is thus
          * superfluous. The next line is not inside the OkPimpAction block because we want to start playback before the
          * body of the request, which may contain a large file, has been received: if the track is already available
          * locally, the uploaded file is ignored. Clients should thus ask the server whether it already has a file before
          * initiating long-running, possibly redundant, file uploads.
          */
        MusicPlayer.setPlaylistAndPlay(track)
        log info s"Playing local file of: ${track.id}"
        pimpAction(Ok)
      }
    }

  //  EssentialAction { req =>
  //    val f: Future[EssentialAction] = ???
  //    Accumulator.flatten(f.map(_.apply(req)))
  //  }

  private def streamingAction(meta: Track): EssentialAction = {
    val relative = meta.path
    // Saves the streamed media to file if possible
    val fileOpt = Library.findAbsoluteNew(relative).filter(canWriteNewFile) orElse
      Option(FileUtilities.tempDir resolve meta.relativePath).filter(canWriteNewFile)
    val (inStream, iteratee) = fileOpt.fold(Streams.joinedStream())(streamingAndFileWritingIteratee)
    val msg = fileOpt.fold(s"Streaming: $relative")(path => s"Streaming: $relative and saving to: $path")
    log info msg
    // Runs on another thread because setPlaylistAndPlay blocks until the InputStream has
    // enough data. Data will only be made available after this call, when the body of
    // the request is parsed (by this same thread, I guess). This Future will complete
    // when setPlaylistAndPlay returns or when the upload is complete, whichever occurs
    // first. When the OutputStream onto which the InputStream is connected is closed,
    // the Future, if still not completed, will complete exceptionally with an IOException.
    Future {
      val track = StreamedTrack.fromTrack(meta, inStream)
      MusicPlayer.setPlaylistAndPlay(track)
    }
    pimpParsedAction(StreamParsers.multiPartBodyParser(iteratee, 1024.megs, errorHandler)(mat)) { _ =>
      log.info(s"Received stream of track: ${meta.id}")
      Ok
    }
  }

  private def canWriteNewFile(file: Path) =
    try {
      val createdFile = Files.createFile(file)
      Files.delete(createdFile)
      true
    } catch {
      case _: Exception => false
    }

  private def loggedJson(errorMessage: String) = {
    log.warn(errorMessage)
    FailReason(errorMessage)
  }

  /** Builds a [[Sink]] that writes any consumed bytes to both `file` and a stream. The bytes
    * written to the stream are made available to the returned [[InputStream]].
    *
    * @param file file to write to
    * @return an [[InputStream]] and a [[Sink]]
    */
  private def streamingAndFileWritingIteratee(file: Path): (PipedInputStream, Sink[ByteString, Future[Long]]) = {
    Option(file.getParent).foreach(p => Files.createDirectories(p))
    val streamOut = new PipedOutputStream()
    val bufferSize = math.min(10.megs.toBytes.toInt, Int.MaxValue)
    val pipeIn = new PipedInputStream(streamOut, bufferSize)
    val fileOut = new BufferedOutputStream(new FileOutputStream(file.toFile))
    val iteratee = Streams.closingStreamWriter(fileOut, streamOut)
    (pipeIn, iteratee)
  }

  private def ackPimpAction[T](parser: BodyParser[T])(bodyHandler: CookiedRequest[T, Username] => Unit): EssentialAction =
    pimpParsedAction(parser) { request =>
      try {
        bodyHandler(request)
        default.AckResponse(request)
      } catch {
        case iae: IllegalArgumentException =>
          log error("Illegal argument", iae)
          badRequest(iae.getMessage)
        case t: Throwable =>
          log error("Unable to execute action", t)
          serverErrorGeneric
      }
    }

  private def jsonAckAction(jsonHandler: CookiedRequest[JsValue, Username] => Unit): EssentialAction =
    ackPimpAction(parsers.json)(jsonHandler)

  private def UploadedSongAction(songAction: PlayableTrack => Unit) =
    metaUploadAction { req =>
      Future.successful {
        songAction(req.track)
        statsPlayer.updateUser(req.username)
        default.AckResponse(req)
      }
    }

  private def metaUploadAction(f: TrackUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => Future[Result]) =
    headPimpUploadAction { request =>
      val parameters = request.body.asFormUrlEncoded

      def firstValue(key: String) = parameters.get(key).flatMap(_.headOption)

      val pathParameterOpt = firstValue("path").map(p => Paths.get(p))
      // if a "path" parameter is specified, attempts to move the uploaded file to that library path
      val absolutePathOpt = pathParameterOpt.flatMap(Library.suggestAbsolute).filter(!Files.exists(_))
      absolutePathOpt.flatMap(p => Option(p.getParent).map(Files.createDirectories(_)))
      val requestFile = request.file
      val file = absolutePathOpt.fold(requestFile)(dest => Files.move(requestFile, dest, StandardCopyOption.REPLACE_EXISTING))
      // attempts to read metadata from file if it was moved to the library, falls back to parameters set in upload
      val trackId = Library.trackId(file)
      val trackInfoFromFileOpt = absolutePathOpt.flatMap(_ => pathParameterOpt.map(p => lib.meta(trackId)))

      def trackInfoFromUpload: LocalTrack = {
        val title = firstValue("title")
        val album = firstValue("album") getOrElse ""
        val artist = firstValue("artist") getOrElse ""
        val meta = SongMeta(StreamSource.fromFile(file), SongTags(title.getOrElse(file.getFileName.toString), album, artist))
        new LocalTrack(Library.trackId(file), UnixPath(file), meta)
      }

      val track = trackInfoFromFileOpt.map(_.map(_.getOrElse(trackInfoFromUpload))).getOrElse(fut(trackInfoFromUpload))
      track.flatMap { t =>
        val user = Username(request.user)
        val mediaInfo = t.meta.media
        val fileSize = mediaInfo.size
        log info s"User: ${request.user} from: ${request.remoteAddress} uploaded $fileSize"
        f(new TrackUploadRequest(t, file, user, request))
      }
    }

  class TrackUploadRequest[A](val track: LocalTrack, file: Path, val username: Username, request: Request[A])
    extends OneFileUploadRequest(file, username.name, request)

}

object Rest {
  private val log = Logger(getClass)

  object trustAllTrustManager extends X509TrustManager {
    override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String) = ()

    override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String) = ()

    override def getAcceptedIssuers: Array[X509Certificate] = Array.empty[X509Certificate]
  }


  val defaultClient = OkClient.default
  val sslClient = OkClient.ssl(SSLUtils.trustAllSslContext().getSocketFactory, trustAllTrustManager)
  val audioMpeg = MediaType.parse("audio/mpeg")

  def close(): Unit = {
    closeOk(defaultClient)
    closeOk(sslClient)
  }

  def closeOk(client: OkClient) = Try {
    client.close()
    val inner = client.client
    inner.dispatcher().cancelAll()
    inner.dispatcher().executorService().shutdownNow()
    val terminated = inner.dispatcher().executorService().awaitTermination(5, TimeUnit.SECONDS)
    if (!terminated) {
      log.error("ExecutorService of HTTP client did not terminate in a timely manner.")
    }
  }

  /** Beams a track to a URI as specified in `cmd`.
    *
    * @param cmd beam details
    */
  def beam(cmd: BeamCommand, lib: MusicLibrary): Future[Either[ErrorMessage, HttpResponse]] = {
    val url = cmd.uri
    lib.findFile(cmd.track).flatMap { maybeFile =>
      maybeFile.map { file =>
        val size = Files.size(file).bytes
        log info s"Beaming: $file of size: $size to: $url..."
        sslClient.multiPart(
          url,
          Map(HeaderNames.AUTHORIZATION -> HttpUtil.authorizationValue(cmd.username.name, cmd.password.pass)),
          files = Seq(MultiPartFile(audioMpeg, file))
        ).map { r =>
          if (r.isSuccess) {
            log info s"Beamed file: $file of size: $size to: $url"
          } else {
            log error s"Beam failed of file: $file of size: $size to: $url"
          }
          Right(r)
        }
      }.getOrElse {
        fut(Left(ErrorMessage(s"Unable to find track with id: ${cmd.track}")))
      }
    }.recover {
      case e: Exception =>
        log.error("Beaming failed.", e)
        Left(ErrorMessage("Beaming failed."))
    }
  }
}
