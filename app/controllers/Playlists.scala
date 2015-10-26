package controllers

import com.mle.musicpimp.models.{PlaylistID, User}
import com.mle.musicpimp.exception.{PimpException, UnauthorizedException}
import com.mle.musicpimp.library.{PlaylistService, PlaylistSubmission}
import com.mle.play.controllers.AuthRequest
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BodyParser, Result}

import scala.concurrent.Future

/**
 * @author mle
 */
class Playlists(service: PlaylistService) extends Secured {

  val Playlist = "playlist"
  val Playlists = "playlists"

  def playlist(id: PlaylistID) = recoveredAsync((req, user) => {
    service.playlist(id, user)
      .map(result => result.map(playlist => Ok(Json.obj(Playlist -> playlist))).getOrElse(NotFound))
  })

  def playlists = recoveredAsync((req, user) => {
    service.playlists(user)
      .map(playlists => Ok(Json.obj(Playlists -> playlists)))
  })

  def savePlaylist = parsedRecoveredAsync(parse.json)((req, user) => {
    val parsedPlaylist = (req.body \ Playlist).validate[PlaylistSubmission]
    parsedPlaylist
      .map(playlist => service.saveOrUpdatePlaylist(playlist, user).map(_ => Accepted))
      .getOrElse(Future.successful(BadRequest))
  })

  def deletePlaylist(id: PlaylistID) = recoveredAsync((req, user) => {
    service.delete(id, user).map(_ => Accepted)
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
  }
}
