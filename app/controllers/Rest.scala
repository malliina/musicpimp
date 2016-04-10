package controllers

import java.io._
import java.net.UnknownHostException
import java.nio.file._

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import com.malliina.audio.ExecutionContexts.defaultPlaybackContext
import com.malliina.audio.meta.{SongMeta, SongTags, StreamSource}
import com.malliina.file.FileUtilities
import com.malliina.http.TrustAllMultipartRequest
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.beam.BeamCommand
import com.malliina.musicpimp.json.{JsonMessages, JsonStrings}
import com.malliina.musicpimp.library.{Library, LocalTrack}
import com.malliina.musicpimp.models.User
import com.malliina.play.Authenticator
import com.malliina.play.controllers.BaseController
import com.malliina.play.http.{AuthRequest, OneFileUploadRequest}
import com.malliina.play.streams.{StreamParsers, Streams}
import com.malliina.storage.{StorageInt, StorageLong}
import com.malliina.util.{Log, Util, Utils}
import org.apache.http.HttpResponse
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.{JsValue, Json}
import play.api.libs.{Files => PlayFiles}
import play.api.mvc._

import scala.concurrent.Future

class Rest(webPlayer: WebPlayer,
           auth: Authenticator,
           handler: PlaybackMessageHandler,
           statsPlayer: StatsPlayer,
           mat: Materializer)
  extends Secured(auth, mat)
    with BaseController
    with Log {

  val webPlayerHandler = webPlayer.messageHandler

  def ping = Action(NoCache(Ok))

  def pingAuth = PimpAction(req => NoCacheOk(JsonMessages.version))

  /**
    * Handles server playback commands POSTed as JSON.
    */
  def playback = JsonAckAction(handler.onJson)

  /**
    * Handles web browser player playback commands POSTed as JSON.
    */
  def webPlayback = JsonAckAction(webPlayerHandler.onJson)

  /**
    * Alias for `playback`. Should be deprecated.
    */
  def playlist = playback

  def playUploadedFile = UploadedSongAction { track =>
    MusicPlayer.setPlaylistAndPlay(track)
  }

  def webPlaylist = PimpAction(req => Ok(Json.toJson(playlistFor(req.user))))

  /**
    * Adds the uploaded track to the server playlist.
    */
  def addUpload = UploadedSongAction { track =>
    MusicPlayer.playlist.add(track)
  }

  /**
    * Starts playback of the track in the request.
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
  def streamedPlayback = Authenticated(user => EssentialAction(requestHeader => {
    val headerValue = requestHeader.headers.get(JsonStrings.TRACK_HEADER)
      .map(Json.parse(_).validate[BaseTrackMeta])
    val metaOrError = headerValue.map(jsonResult => jsonResult.fold(
      invalid => Left(loggedJson(s"Invalid JSON: $invalid")),
      valid => Right(valid))
    ).getOrElse(Left(loggedJson("No Track header is defined.")))
    val authAction = metaOrError.fold(
      error => PimpAction(BadRequest(error)),
      meta => {
        statsPlayer.updateUser(User(user.user))
        localPlaybackAction(meta.id).getOrElse(streamingAction(meta))
      }
    )
    authAction(requestHeader)
  }))

  /** Beams the local track according to the [[BeamCommand]] in the request body.
    *
    * TODO: if no root folder for the track is found this shit explodes, fix and return an erroneous HTTP response instead
    */
  def stream = PimpParsedAction(parse.json)(implicit req => {
    Json.fromJson[BeamCommand](req.body).fold(
      invalid = jsonErrors => BadRequest(JsonMessages.invalidJson),
      valid = cmd => {
        try {
          val (response, duration) = Utils.timed(Rest.beam(cmd))
          response.fold(
            errorMsg => BadRequest(JsonMessages.failure(errorMsg)),
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
            NotFound(JsonMessages.failure(s"Unable to find MusicBeamer endpoint. ${uhe.getMessage}"))
          case e: Exception =>
            InternalServerError
        }
      }
    )
  })

  def status = PimpAction(implicit req => pimpResponse(
    html = NoContent,
    json17 = Json.toJson(MusicPlayer.status17),
    latest = Json.toJson(MusicPlayer.status)
  ))

  /**
    * The status of the web player of the user making the request.
    */
  def webStatus = PimpAction(req => Ok(webStatusJson(req.user)))

  private def localPlaybackAction(id: String): Option[EssentialAction] =
    Library.findMetaWithTempFallback(id).map(track => {
      /**
        * The MusicPlayer is intentionally modified outside of the PimpAction block. Here's why this is correct:
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
      PimpAction(Ok)
    })

  private def streamingAction(meta: BaseTrackMeta): EssentialAction = {
    val relative = Library.relativePath(meta.id)
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
      val track = meta.buildTrack(inStream)
      MusicPlayer.setPlaylistAndPlay(track)
    }
    PimpParsedAction(StreamParsers.multiPartBodyParser(iteratee, 1024.megs)(mat))(req => {
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

  /** Builds an [[play.api.libs.iteratee.Iteratee]] that writes any consumed bytes to both `file` and a stream. The bytes
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

  private def webStatusJson(user: String) = {
    val player = webPlayer.players.getOrElse(user, new PimpWebPlayer(user, webPlayer))
    Json.toJson(player.status)
  }

  private def playlistFor(user: String): Seq[TrackMeta] =
    webPlayer.players.get(user).fold(Seq.empty[TrackMeta])(_.playlist.songList)

  private def AckPimpAction[T](parser: BodyParser[T])(bodyHandler: AuthRequest[T] => Unit): EssentialAction =
    PimpParsedAction(parser)(implicit request => {
      try {
        bodyHandler(request)
        AckResponse
      } catch {
        case iae: IllegalArgumentException =>
          log error("Illegal argument", iae)
          val errorMessage = JsonMessages.failure(iae.getMessage)
          BadRequest(errorMessage)
        case t: Throwable =>
          log error("Unable to execute action", t)
          InternalServerError
      }
    })

  private def JsonAckAction(jsonHandler: AuthRequest[JsValue] => Unit): EssentialAction =
    AckPimpAction(parse.json)(jsonHandler)

  private def UploadedSongAction(songAction: PlayableTrack => Unit) =
    MetaUploadAction(implicit req => {
      songAction(req.track)
      statsPlayer.updateUser(User(req.user))
      AckResponse
    })

  private def MetaUploadAction(f: TrackUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => Result) =
    HeadPimpUploadAction(request => {
      val parameters = request.body.asFormUrlEncoded
      def firstValue(key: String) = parameters.get(key).flatMap(_.headOption)
      val pathParameterOpt = firstValue("path")
      // if a "path" parameter is specified, attempts to move the uploaded file to that library path
      val absolutePathOpt = pathParameterOpt.flatMap(Library.suggestAbsolute).filter(!Files.exists(_))
      absolutePathOpt.flatMap(p => Option(p.getParent).map(Files.createDirectories(_)))
      val requestFile = request.file
      val file = absolutePathOpt.fold(requestFile)(dest => Files.move(requestFile, dest, StandardCopyOption.REPLACE_EXISTING))
      // attempts to read metadata from file if it was moved to the library, falls back to parameters set in upload
      val trackInfoFromFileOpt = absolutePathOpt.flatMap(_ => pathParameterOpt.flatMap(Library.findMeta))
      def trackInfoFromUpload: LocalTrack = {
        val title = firstValue("title")
        val album = firstValue("album") getOrElse ""
        val artist = firstValue("artist") getOrElse ""
        val meta = SongMeta(StreamSource.fromFile(file), SongTags(title.getOrElse(file.getFileName.toString), album, artist))
        new LocalTrack("", meta)
      }
      val track = trackInfoFromFileOpt getOrElse trackInfoFromUpload
      val user = request.user
      val mediaInfo = track.meta.media
      val fileSize = mediaInfo.size
      log info s"User: ${request.user} from: ${request.remoteAddress} uploaded $fileSize"
      f(new TrackUploadRequest(track, file, user, request))
    })

  class TrackUploadRequest[A](val track: LocalTrack, file: Path, user: String, request: Request[A])
    extends OneFileUploadRequest(file, user, request)

}

object Rest extends Log {
  /**
    * Beams a track to a URI as specified in `cmd`.
    *
    * @param cmd beam details
    */
  def beam(cmd: BeamCommand): Either[String, HttpResponse] =
    try {
      val uri = cmd.uri
      Util.using(new TrustAllMultipartRequest(uri))(req => {
        req.setAuth(cmd.username, cmd.password)
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
