package controllers

import com.mle.musicpimp.exception.{PimpException, UnauthorizedException}
import com.mle.musicpimp.json.JsonStrings.PlaylistKey
import com.mle.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.mle.musicpimp.models.{PlaylistID, User}
import com.mle.play.controllers.AuthRequest
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BodyParser, Result}
import views.html

import scala.concurrent.Future

object Playlists {
  val Id = "id"
  val Name = "name"
  val Tracks = "tracks"
}

/**
 * @author mle
 */
class Playlists(service: PlaylistService) extends Secured {

  val playlistIdField: Mapping[PlaylistID] = longNumber.transform(l => PlaylistID(l), id => id.id)
  val playlistForm: Form[PlaylistSubmission] = Form(mapping(
    Playlists.Id -> optional(playlistIdField),
    Playlists.Name -> nonEmptyText,
    Playlists.Tracks -> seq(text)
  )(PlaylistSubmission.apply)(PlaylistSubmission.unapply))

  def playlists = recoveredAsync((req, user) => {
    service.playlistsMeta(user).map(playlists => {
      respond(
        html = html.playlists(playlists.playlists),
        json = Json.toJson(playlists)
      )(req)
    })
  })

  def playlist(id: PlaylistID) = recoveredAsync((req, user) => {
    service.playlistMeta(id, user).map(result => {
      result.map(playlist => respond(
        html = html.playlist(playlist.playlist, playlistForm),
        json = Json.toJson(playlist)
      )(req)).getOrElse(NotFound(s"Playlist not found: $id"))
    })
  })

  def savePlaylist = parsedRecoveredAsync(parse.json)((req, user) => {
    val json = req.body
    (json \ PlaylistKey).validate[PlaylistSubmission]
      .map(playlist => service.saveOrUpdatePlaylistMeta(playlist, user).map(meta => Accepted(Json.toJson(meta))))
      .getOrElse(Future.successful(BadRequest(s"Invalid JSON: $json")))
  })

  def deletePlaylist(id: PlaylistID) = recoveredAsync((req, user) => {
    service.delete(id, user).map(_ => Accepted)
  })

  def edit = parsedRecoveredAsync(parse.json)((req, user) => {
    Future.successful(Ok)
  })

  def handleSubmission = recoveredAsync((req, user) => {
    playlistForm.bindFromRequest()(req).fold(
      errors => {
        service.playlistsMeta(user).map(pls => BadRequest(html.playlists(pls.playlists)))
      },
      submission => {
        Future.successful(Redirect(routes.Playlists.playlists()))
      }
    )
  })

  protected def recoveredAsync(f: (AuthRequest[AnyContent], User) => Future[Result]) =
    parsedRecoveredAsync(parse.anyContent)(f)

  protected def parsedRecoveredAsync[T](parser: BodyParser[T] = parse.anyContent)(f: (AuthRequest[T], User) => Future[Result]) =
    PimpParsedActionAsync(parser)(auth => f(auth, User(auth.user)).recover(errorHandler))

  override def errorHandler: PartialFunction[Throwable, Result] = {
    case ue: UnauthorizedException =>
      log.error(s"Unauthorized", ue)
      Unauthorized
    case pe: PimpException =>
      log.error(s"Pimp error", pe)
      InternalServerError
    case t: Throwable =>
      log.error(s"Server error", t)
      InternalServerError
  }
}
