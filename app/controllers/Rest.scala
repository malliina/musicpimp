package controllers

import play.api.mvc._
import java.nio.file._
import com.mle.musicpimp.audio._
import com.mle.util.{FileUtilities, Utils, Util, Log}
import com.mle.musicpimp.json.{JsonStrings, JsonMessages}
import play.api.libs.{Files => PlayFiles}
import play.api.libs.json.{Json, JsValue}
import com.mle.audio.meta.{StreamSource, SongTags, SongMeta}
import com.mle.musicpimp.library.{Library, LocalTrack}
import com.mle.http.TrustAllMultipartRequest
import org.apache.http.HttpResponse
import com.mle.play.controllers.{OneFileUploadRequest, AuthRequest, BaseController}
import java.net.UnknownHostException
import com.mle.play.streams.{Streams, StreamParsers}
import com.mle.musicpimp.beam.BeamCommand
import scala.concurrent.Future
import java.io._
import com.mle.storage.StorageInt
import com.mle.audio.ExecutionContexts.defaultPlaybackContext
import play.api.mvc.SimpleResult

/**
 *
 * @author mle
 */
object Rest
  extends Secured
  with BaseController
  with LibraryController
  with PimpContentController
  with Log {

  def ping = Action(NoCache(Ok))

  def pingAuth = PimpAction(req => NoCacheOk(JsonMessages.Version))

  def playback = JsonAckAction(PlaybackMessageHandler.onJson)

  def webPlayback = JsonAckAction(WebPlayerMessageHandler.onJson)

  def playlist = JsonAckAction(PlaybackMessageHandler.onJson)

  def playUploadedFile = UploadedSongAction(MusicPlayer.setPlaylistAndPlay)

  /**
   * First authenticates, then reads the Track header, then attempts to find the track ID from
   * local storage. If found, starts playback of the local track, otherwise, starts playback of
   * the streamed track available in the body of the request.
   *
   * Returns Unauthorized if authentication fails (as usual), and BadRequest with JSON if the
   * Track header is faulty. Otherwise returns 200 OK.
   *
   * @return
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
      meta => localPlaybackAction(meta.id).getOrElse(streamingAction(meta)))
    authAction(requestHeader)
  }))

  private def localPlaybackAction(id: String): Option[EssentialAction] =
    Library.findMetaWithTempFallback(id).map(track =>
      OkPimpAction(implicit req => {
        log.info(s"Playing local file of: ${track.id}")
        MusicPlayer.setPlaylistAndPlay(track)
      }))

  private def streamingAction(meta: BaseTrackMeta): EssentialAction = {
    val relative = Library.relativePath(meta.id)
    val file = FileUtilities.tempDir resolve relative
    val (inStream, iteratee) = streamingAndFileWritingIteratee(file)
    log.info(s"Playing stream of: ${meta.id}")
    // Run in another thread because setPlaylistAndPlay blocks until the InputStream has
    // enough data. Data will only be made available after this call, when the body of
    // the request is parsed (by this same thread, I guess). This Future will complete
    // when setPlaylistAndPlay returns or when the upload is complete, whichever occurs
    // first. When the OutputStream onto which the InputStream is connected is closed,
    // the Future, if still not completed, will complete exceptionally with an IOException.
    Future {
      val track = meta.buildTrack(inStream)
      MusicPlayer.setPlaylistAndPlay(track)
    }
    PimpParsedAction(StreamParsers.multiPartBodyParser(iteratee))(req => {
      log.info(s"Received stream of track: ${meta.id}")
      Ok
    })
  }

  def loggedJson(errorMessage: String) = {
    log.warn(errorMessage)
    JsonMessages.failure(errorMessage)
  }

  /**
   * Builds an [[play.api.libs.iteratee.Iteratee]] that writes any consumed bytes to both `file` and a stream. The bytes
   * written to the stream are made available to the returned [[InputStream]].
   *
   * @param file file to write to
   * @return an [[InputStream]] an an [[play.api.libs.iteratee.Iteratee]]
   */
  def streamingAndFileWritingIteratee(file: Path) = {
    Option(file.getParent).foreach(p => Files.createDirectories(p))
    val streamOut = new PipedOutputStream()
    val bufferSize = math.min(10.megs.toBytes.toInt, Int.MaxValue)
    val pipeIn = new PipedInputStream(streamOut, bufferSize)
    val fileOut = new BufferedOutputStream(new FileOutputStream(file.toFile))
    val iteratee = Streams.closingStreamWriter(fileOut, streamOut)
    (pipeIn, iteratee)
  }

  // TODO if no root folder for the track is found this shit explodes, fix and return an erroneous HTTP response instead
  def stream = PimpParsedAction(parse.json)(implicit req => {
    Json.fromJson[BeamCommand](req.body).fold(
      invalid = jsonErrors => BadRequest(JsonMessages.InvalidJson),
      valid = cmd => {
        try {
          val (response, duration) = Utils.timed(beam(cmd))
          response.fold(
            errorMsg => BadRequest(JsonMessages.failure(errorMsg)),
            httpResponse => {
              // relays the response code of the request to the beam endpoint to the client
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

  /**
   * Beams a track to a URI as specified in the supplied command details.
   *
   * @param cmd beam details
   */
  private def beam(cmd: BeamCommand): Either[String, HttpResponse] =
    try {
      val uri = cmd.uri
      Util.using(new TrustAllMultipartRequest(uri))(req => {
        req.setAuth(cmd.username, cmd.password)
        Library.findAbsolute(cmd.track).map(file => {
          req addFile file
          val response = req.execute()
          log info s"Uploaded file: $file, bytes: ${Files.size(file)} to: $uri"
          Right(response)
        }).getOrElse(Left(s"Unable to find track with id: ${cmd.track}"))
      })
    } catch {
      case e: Throwable =>
        log.warn("Unable to beam", e)
        Left("An error occurred while MusicBeaming. Please check your settings or try again later.")
    }

  def addUpload = UploadedSongAction(MusicPlayer.playlist.add)

  def status = PimpAction(implicit req => pimpResponse(
    html = NoContent,
    json17 = Json.toJson(MusicPlayer.status17),
    latest = Json.toJson(MusicPlayer.status)
  ))

  def webStatus = PimpAction(req => Ok(webStatusJson(req.user)))

  private def webStatusJson(user: String) = {
    val player = WebPlayback.players.get(user) getOrElse new PimpWebPlayer(user)
    Json.toJson(player.status)
  }

  def webPlaylist = PimpAction(req => Ok(Json.toJson(playlistFor(req.user))))

  private def playlistFor(user: String): Seq[TrackMeta] =
    WebPlayback.players.get(user).map(_.playlist.songList) getOrElse Seq.empty

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
      AckResponse
    })

  private def MetaUploadAction(f: TrackUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => SimpleResult) =
    HeadPimpUploadAction(request => {
      val parameters = request.body.asFormUrlEncoded
      def firstValue(key: String) = parameters.get(key).flatMap(_.headOption)
      val pathParameterOpt = firstValue("path")
      // if a "path" parameter is specified, attempts to move the uploaded file to that library path
      val absolutePathOpt = pathParameterOpt.flatMap(Library.suggestAbsolute).filter(!Files.exists(_))
      absolutePathOpt.flatMap(p => Option(p.getParent).map(Files.createDirectories(_)))
      val requestFile = request.file
      val file = absolutePathOpt
        .map(dest => Files.move(requestFile, dest, StandardCopyOption.REPLACE_EXISTING))
        .getOrElse(requestFile)
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

//  def pingAuth = Action(req => {
//    val headers = req.headers.toSimpleMap.map(kv => kv._1+"="+kv._2).mkString("\n")
//    log info s"Headers: \n$headers"
//    NoCacheOk(JsonMessages.Version)
//  })

