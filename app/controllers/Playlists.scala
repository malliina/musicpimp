package controllers

import akka.stream.Materializer
import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.exception.{PimpException, UnauthorizedException}
import com.malliina.musicpimp.json.JsonStrings.PlaylistKey
import com.malliina.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.{PimpUrl, PlaylistID, PlaylistsMeta, User}
import com.malliina.play.Authenticator
import com.malliina.play.http.AuthRequest
import controllers.Playlists.log
import play.api.Logger
import play.api.data.Forms._
import play.api.data.{Form, Mapping}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BodyParser, Result}
import views.html

import scala.concurrent.Future

object Playlists {
  private val log = Logger(getClass)
  val Id = "id"
  val Name = "name"
  val Tracks = "tracks"
}

class Playlists(service: PlaylistService, auth: Authenticator, mat: Materializer) extends Secured(auth, mat) {

  val playlistIdField: Mapping[PlaylistID] = longNumber.transform(l => PlaylistID(l), id => id.id)
  val playlistForm: Form[PlaylistSubmission] = Form(mapping(
    Playlists.Id -> optional(playlistIdField),
    Playlists.Name -> nonEmptyText,
    Playlists.Tracks -> seq(text)
  )(PlaylistSubmission.apply)(PlaylistSubmission.unapply))

  def playlists = recoveredAsync((req, user) => {
    implicit val f = PlaylistsMeta.format(TrackMeta.format(req))
    service.playlistsMeta(user).map(playlists => {
      respond(req)(
        html = html.playlists(playlists.playlists),
        json = Json.toJson(playlists)
      )
    })
  })

  def playlist(id: PlaylistID) = recoveredAsync((req, user) => {
    implicit val f = TrackMeta.format(req)
    service.playlistMeta(id, user).map { result =>
      result.map { playlist =>
        respond(req)(
          html = html.playlist(playlist.playlist, playlistForm),
          json = Json.toJson(playlist)
        )
      }.getOrElse(NotFound(s"Playlist not found: $id"))
    }
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
    pimpParsedActionAsync(parser)(auth => f(auth, User(auth.user)).recover(errorHandler))

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
