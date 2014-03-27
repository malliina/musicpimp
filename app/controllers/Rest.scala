package controllers

import play.api.mvc._
import java.nio.file._
import com.mle.musicpimp.audio._
import com.mle.util.{Util, Log}
import com.mle.musicpimp.json.JsonMessages
import play.api.libs.{Files => PlayFiles}
import play.api.libs.json.{Json, JsValue}
import com.mle.audio.meta.{MediaInfo, SongTags, SongMeta}
import com.mle.musicpimp.library.{Library, TrackInfo}
import com.mle.musicpimp.beam.BeamCommand
import com.mle.http.TrustAllMultipartRequest
import org.apache.http.HttpResponse
import com.mle.play.controllers.{OneFileUploadRequest, AuthRequest, BaseController}
import java.net.UnknownHostException

/**
 *
 * @author mle
 */
class TrackUploadRequest[A](val track: TrackInfo, file: Path, user: String, request: Request[A])
  extends OneFileUploadRequest(file, user, request)

object Rest
  extends Secured
  with BaseController
  with LibraryController
  with PimpContentController
  with Log {

  def ping = Action(NoCache(Ok))

  //  def pingAuth = Action(req => {
  //    val headers = req.headers.toSimpleMap.map(kv => kv._1+"="+kv._2).mkString("\n")
  //    log info s"Headers: \n$headers"
  //    NoCacheOk(JsonMessages.Version)
  //  })
  def pingAuth = PimpAction(req => NoCacheOk(JsonMessages.Version))

  def playback = JsonAckAction(req => {
    // LineUnavailableException may propagate here if playback cannot be started
    JsonMessageHandler.onPlaybackCommand(req.body)
  })

  def webPlayback = JsonAckAction(req => {
    JsonMessageHandler.onClientMessage(req.user, req.body)
  })

  def playlist = JsonAckAction(req => {
    JsonMessageHandler.onPlaybackCommand(req.body)
  })

  def track = UploadedSongAction(setPlaylist)

  def playUploadedFile = UploadedSongAction(track => {
    setPlaylist(track)
    MusicPlayer.playTrack(track)
  })

  private def measureTime[T](f: => T): (T, Long) = {
    val start = System.currentTimeMillis()
    val ret = f
    val end = System.currentTimeMillis()
    (ret, end - start)
  }

  // TODO if no root folder for the track is found this shit explodes, fix and return an erroneous HTTP response instead
  def stream = PimpParsedAction(parse.json)(implicit req => {
    implicit val commandFormat = Json.format[BeamCommand]
    Json.fromJson[BeamCommand](req.body).fold(
      invalid = jsonErrors => BadRequest(JsonMessages.InvalidJson),
      valid = cmd => {
        try {
          val (response, duration) = measureTime(beam(cmd))
          response.fold(
            errorMsg => BadRequest(JsonMessages.failure(errorMsg)),
            httpResponse => {
              // relays the response code of the request to the beam endpoint to the client
              val statusCode = httpResponse.getStatusLine.getStatusCode
              log info s"Completed track upload in: $duration ms, relaying response: $statusCode"
              if (statusCode == 200) {
                new Status(statusCode)(Json.obj("msg" -> Json.toJson("thank you")))
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
        Library.toAbsolute(cmd.track).map(file => {
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
    json18 = Json.toJson(MusicPlayer.status),
    json17 = Json.toJson(MusicPlayer.status17)
  ))

  def webStatus = PimpAction(req => Ok(webStatusJson(req.user)))

  private def webStatusJson(user: String) = {
    val player = WebPlayback.players.get(user) getOrElse new PimpWebPlayer(user)
    Json.toJson(player.status)
  }

  def webPlaylist = PimpAction(req => Ok(Json.toJson(playlistFor(req.user))))

  private def playlistFor(user: String) =
    WebPlayback.players.get(user).map(_.playlist.songList) getOrElse Seq.empty

  private def setPlaylist(track: TrackInfo) {
    MusicPlayer.playlist.clear()
    MusicPlayer.playlist.add(track)
  }

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

  private def UploadedSongAction(songAction: TrackInfo => Unit) =
    MetaUploadAction(implicit req => {
      songAction(req.track)
      AckResponse
    })

  private def MetaUploadAction(f: TrackUploadRequest[MultipartFormData[PlayFiles.TemporaryFile]] => SimpleResult) =
    HeadPimpUploadAction(request => {
      val parameters = request.body.asFormUrlEncoded
      def firstValue(key: String) = parameters.get(key).flatMap(_.headOption)
      val title = firstValue("title")
      val album = firstValue("album") getOrElse ""
      val artist = firstValue("artist") getOrElse ""
      val file = request.file
      val meta = SongMeta(MediaInfo.fromPath(file), SongTags(title.getOrElse(file.getFileName.toString), album, artist))
      val track = new TrackInfo("", meta)
      val user = request.user
      log info s"User: ${request.user} from: ${request.remoteAddress} uploaded ${meta.media.size.toBytes} bytes to: ${meta.media.uri}"
      f(new TrackUploadRequest(track, file, user, request))
    })
}
