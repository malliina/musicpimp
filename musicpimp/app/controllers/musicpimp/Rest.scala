package controllers.musicpimp

import java.io._
import java.net.UnknownHostException
import java.nio.file._

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.malliina.audio.meta.{SongMeta, SongTags, StreamSource}
import com.malliina.file.FileUtilities
import com.malliina.http.TrustAllMultipartRequest
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.beam.BeamCommand
import com.malliina.musicpimp.json.{JsonMessages, JsonStrings}
import com.malliina.musicpimp.library.{Library, LocalTrack}
import com.malliina.musicpimp.models._
import com.malliina.play.CookieAuthenticator
import com.malliina.play.controllers.Caching.{NoCache, NoCacheOk}
import com.malliina.play.http.{CookiedRequest, OneFileUploadRequest}
import com.malliina.play.models.Username
import com.malliina.play.streams.{StreamParsers, Streams}
import com.malliina.storage.{StorageInt, StorageLong}
import com.malliina.util.{Util, Utils}
import controllers.musicpimp.Rest.log
import org.apache.http.HttpResponse
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.libs.{Files => PlayFiles}
import play.api.mvc._

import scala.concurrent.Future

class Rest(webPlayer: WebPlayer,
           auth: CookieAuthenticator,
           handler: PlaybackMessageHandler,
           statsPlayer: StatsPlayer,
           mat: Materializer)
  extends Secured(auth, mat) {

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
  def streamedPlayback = authenticated { req =>
    EssentialAction { requestHeader =>
      val headerValue = requestHeader.headers.get(JsonStrings.TrackHeader)
        .map(Json.parse(_).validate[Track])
      val metaOrError = headerValue.map(jsonResult => jsonResult.fold(
        invalid => Left(loggedJson(s"Invalid JSON: $invalid")),
        valid => Right(valid))
      ).getOrElse(Left(loggedJson("No Track header is defined.")))
      val authAction = metaOrError.fold(
        error => pimpAction(BadRequest(error)),
        meta => {
          statsPlayer.updateUser(req.user)
          localPlaybackAction(meta.id).getOrElse(streamingAction(meta))
        }
      )
      authAction(requestHeader)
    }
  }

  /** Beams the local track according to the [[BeamCommand]] in the request body.
    *
    * TODO: if no root folder for the track is found this shit explodes, fix and return an erroneous HTTP response instead
    */
  def stream = pimpParsedAction(parse.json) { req =>
    Json.fromJson[BeamCommand](req.body).fold(
      invalid = _ => BadRequest(JsonMessages.invalidJson),
      valid = cmd => {
        try {
          val (response, duration) = Utils.timed(Rest.beam(cmd))
          response.fold(
            errorMsg => badRequest(errorMsg),
            httpResponse => {
              // relays MusicBeamer's response to the client
              val statusCode = httpResponse.getStatusLine.getStatusCode
              log info s"Completed track upload in: $duration, relaying response: $statusCode"
              if (statusCode == OK) {
                new Status(statusCode)(JsonMessages.thanks)
              } else {
                new Status(statusCode)
              }
            })
        } catch {
          case uhe: UnknownHostException =>
            notFound(s"Unable to find MusicBeamer endpoint. ${uhe.getMessage}")
          case e: Exception =>
            log.error("Stream failure", e)
            serverError("Stream failure")
        }
      }
    )
  }

  def status = pimpAction { req =>
    implicit val w = TrackJson.writer(req)
    pimpResponse(req)(
      html = NoContent,
      json17 = Json.toJson(MusicPlayer.status17),
      latest = Json.toJson(MusicPlayer.status)
    )
  }

  private def localPlaybackAction(id: TrackID): Option[EssentialAction] =
    Library.findMetaWithTempFallback(id) map { track =>
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

  private def streamingAction(meta: Track): EssentialAction = {
    val relative = PimpEnc.relativePath(meta.id)
    // Saves the streamed media to file if possible
    val fileOpt = Library.suggestAbsolute(relative).filter(canWriteNewFile) orElse
      Option(FileUtilities.tempDir resolve relative).filter(canWriteNewFile)
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
    pimpParsedAction(StreamParsers.multiPartBodyParser(iteratee, 1024.megs)(mat))(req => {
      log.info(s"Received stream of track: ${meta.id}")
      Ok
    })
  }

  private def canWriteNewFile(file: Path) =
    try {
      val createdFile = Files.createFile(file)
      Files.delete(createdFile)
      true
    } catch {
      case e: Exception => false
    }

  private def loggedJson(errorMessage: String) = {
    log.warn(errorMessage)
    JsonMessages.failure(errorMessage)
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
        AckResponse(request)
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
    ackPimpAction(parse.json)(jsonHandler)

  private def UploadedSongAction(songAction: PlayableTrack => Unit) =
    metaUploadAction { req =>
      songAction(req.track)
      statsPlayer.updateUser(req.username)
      AckResponse(req)
    }

  private def metaUploadAction(f: TrackUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => Result) =
    headPimpUploadAction { request =>
      val parameters = request.body.asFormUrlEncoded
      def firstValue(key: String) = parameters.get(key).flatMap(_.headOption)
      val pathParameterOpt = firstValue("path").map(TrackID.apply)
      // if a "path" parameter is specified, attempts to move the uploaded file to that library path
      val absolutePathOpt = pathParameterOpt.flatMap(Library.suggestAbsolute).filter(!Files.exists(_))
      absolutePathOpt.flatMap(p => Option(p.getParent).map(Files.createDirectories(_)))
      val requestFile = request.file
      val file = absolutePathOpt.fold(requestFile)(dest => Files.move(requestFile, dest, StandardCopyOption.REPLACE_EXISTING))
      // attempts to read metadata from file if it was moved to the library, falls back to parameters set in upload
      val trackInfoFromFileOpt = absolutePathOpt.flatMap(_ => pathParameterOpt.flatMap(p => Library.findMeta(p)))
      def trackInfoFromUpload: LocalTrack = {
        val title = firstValue("title")
        val album = firstValue("album") getOrElse ""
        val artist = firstValue("artist") getOrElse ""
        val meta = SongMeta(StreamSource.fromFile(file), SongTags(title.getOrElse(file.getFileName.toString), album, artist))
        new LocalTrack(TrackID(""), PimpPath.Empty, meta)
      }
      val track = trackInfoFromFileOpt getOrElse trackInfoFromUpload
      val user = Username(request.user)
      val mediaInfo = track.meta.media
      val fileSize = mediaInfo.size
      log info s"User: ${request.user} from: ${request.remoteAddress} uploaded $fileSize"
      f(new TrackUploadRequest(track, file, user, request))
    }

  class TrackUploadRequest[A](val track: LocalTrack, file: Path, val username: Username, request: Request[A])
    extends OneFileUploadRequest(file, username.name, request)

}

object Rest {
  private val log = Logger(getClass)

  /** Beams a track to a URI as specified in `cmd`.
    *
    * @param cmd beam details
    */
  def beam(cmd: BeamCommand): Either[String, HttpResponse] =
    try {
      val uri = cmd.uri
      Util.using(new TrustAllMultipartRequest(uri))(req => {
        req.setAuth(cmd.username.name, cmd.password.pass)
        Library.findAbsolute(cmd.track).map(file => {
          val size = Files.size(file).bytes
          log info s"Beaming: $file of size: $size to: $uri..."
          req addFile file
          val response = req.execute()
          log info s"Beamed file: $file of size: $size to: $uri"
          Right(response)
        }).getOrElse(Left(s"Unable to find track with id: ${cmd.track}"))
      })
    } catch {
      case e: Throwable =>
        log.warn("Unable to beam.", e)
        Left("An error occurred while MusicBeaming. Please check your settings or try again later.")
    }
}
