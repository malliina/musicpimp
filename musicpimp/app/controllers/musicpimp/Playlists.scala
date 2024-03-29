package controllers.musicpimp

import com.malliina.musicpimp.audio.TrackJson
import com.malliina.musicpimp.exception.{PimpException, UnauthorizedException}
import com.malliina.musicpimp.html.PimpHtml
import com.malliina.musicpimp.http.PimpContentController.default
import com.malliina.musicpimp.json.JsonStrings.PlaylistKey
import com.malliina.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.{Errors, PlaylistID, TrackID}
import com.malliina.play.http.CookiedRequest
import com.malliina.values.Username
import controllers.musicpimp.Playlists.log
import play.api.Logger
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BodyParser, Result, Results}

import scala.concurrent.Future

object Playlists {
  private val log = Logger(getClass)
  val Id = "id"
  val Name = "name"
  val Tracks = "tracks"
}

class Playlists(tags: PimpHtml, service: PlaylistService, auth: AuthDeps) extends Secured(auth) {
  val playlistIdField: Mapping[PlaylistID] = longNumber.transform(l => PlaylistID(l), id => id.id)
  val tracksMapping: Mapping[Seq[TrackID]] =
    seq(text).transform(ss => ss.map(TrackID.apply), ts => ts.map(_.id))
  val playlistForm: Form[PlaylistSubmission] = Form(
    mapping(
      Playlists.Id -> optional(playlistIdField),
      Playlists.Name -> nonEmptyText,
      Playlists.Tracks -> tracksMapping
    )(PlaylistSubmission.apply)(PlaylistSubmission.unapply)
  )

  def playlists = recoveredAsync { req =>
    val user = req.user
    service.playlistsMeta(user).map { playlists =>
      default.respond(req)(
        html = tags.playlists(playlists.playlists, user),
        json = Json.toJson(TrackJson.toFullPlaylistsMeta(playlists, TrackJson.host(req)))
      )
    }
  }

  def playlist(id: PlaylistID) = recoveredAsync { req =>
    val user = req.user
    service.playlistMeta(id, user).map { result =>
      result.map { playlist =>
        default.respond(req)(
          html = tags.playlist(playlist.playlist, user),
          json = Json.toJson(TrackJson.toFullMeta(playlist, TrackJson.host(req)))
        )
      }.getOrElse(notFound(s"Playlist not found: $id"))
    }
  }

  def savePlaylist = parsedRecoveredAsync(parsers.json) { req =>
    val json = req.body
    (json \ PlaylistKey)
      .validate[PlaylistSubmission]
      .map(playlist =>
        service
          .saveOrUpdatePlaylistMeta(playlist, req.user)
          .map(meta => Accepted(Json.toJson(meta)))
      )
      .getOrElse(fut(badRequest(s"Invalid JSON: $json")))
  }

  def deletePlaylist(id: PlaylistID) = recoveredAsync { req =>
    service.delete(id, req.user).map(_ => Accepted)
  }

  def edit = parsedRecoveredAsync(parsers.json) { req => fut(Ok) }

  def handleSubmission = recoveredAsync { req =>
    val user = req.user
    playlistForm
      .bindFromRequest()(req, formBinding)
      .fold(
        errors => {
          service.playlistsMeta(user).map(pls => BadRequest(tags.playlists(pls.playlists, user)))
        },
        submission => {
          fut(Redirect(routes.Playlists.playlists))
        }
      )
  }

  protected def recoveredAsync(f: CookiedRequest[AnyContent, Username] => Future[Result]) =
    parsedRecoveredAsync(parsers.anyContent)(f)

  protected def parsedRecoveredAsync[T](
    parser: BodyParser[T]
  )(f: CookiedRequest[T, Username] => Future[Result]) =
    pimpParsedActionAsync(parser)(auth => f(auth).recover(errorHandler))

  override def errorHandler: PartialFunction[Throwable, Result] = {
    case ue: UnauthorizedException =>
      log.error(s"Unauthorized", ue)
      Errors.withStatus(Results.Unauthorized, "Access denied")
    case pe: PimpException =>
      log.error(s"Pimp error", pe)
      serverErrorGeneric
    case t: Throwable =>
      log.error(s"Server error", t)
      serverErrorGeneric
  }
}
