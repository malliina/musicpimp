package controllers.musicpimp

import cats.effect.IO
import com.malliina.concurrent.Execution.runtime
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
import play.api.data.Forms.*
import play.api.data.{Form, Mapping}
import play.api.mvc.{AnyContent, BodyParser, Result, Results}

import scala.concurrent.Future

object Playlists:
  private val log = Logger(getClass)
  val Id = "id"
  val Name = "name"
  val Tracks = "tracks"

class Playlists(tags: PimpHtml, service: PlaylistService[IO], auth: AuthDeps) extends Secured(auth):
  val playlistIdField: Mapping[PlaylistID] = longNumber.transform(l => PlaylistID(l), id => id.id)
  val tracksMapping: Mapping[Seq[TrackID]] =
    seq(text).transform(ss => ss.map(TrackID.apply), ts => ts.map(_.id))
  val playlistForm: Form[PlaylistSubmission] = Form(
    mapping(
      Playlists.Id -> optional(playlistIdField),
      Playlists.Name -> nonEmptyText,
      Playlists.Tracks -> tracksMapping
    )(PlaylistSubmission.apply)(p => Option((p.id, p.name, p.tracks)))
  )

  def playlists = recoveredAsync: req =>
    val user = req.user
    service
      .playlistsMeta(user)
      .map: playlists =>
        default.respond(req)(
          html = tags.playlists(playlists.playlists, user),
          json = TrackJson.toFullPlaylistsMeta(playlists, TrackJson.host(req))
        )

  def playlist(id: PlaylistID) = recoveredAsync: req =>
    val user = req.user
    service
      .playlistMeta(id, user)
      .map: result =>
        result
          .map: playlist =>
            default.respond(req)(
              html = tags.playlist(playlist.playlist, user),
              json = TrackJson.toFullMeta(playlist, TrackJson.host(req))
            )
          .getOrElse(notFound(s"Playlist not found: $id"))

  def savePlaylist = parsedRecoveredAsync(circeJson): req =>
    val json = req.body
    json.hcursor
      .downField(PlaylistKey)
      .as[PlaylistSubmission]
      .map(playlist =>
        service
          .saveOrUpdatePlaylistMeta(playlist, req.user)
          .map(meta => Accepted(meta))
      )
      .getOrElse(IO.pure(badRequest(s"Invalid JSON: $json")))

  def deletePlaylist(id: PlaylistID) = recoveredAsync: req =>
    service.delete(id, req.user).map(_ => Accepted)

  def edit = parsedRecoveredAsync(parsers.json): req =>
    IO.pure(Ok)

  def handleSubmission = recoveredAsync: req =>
    val user = req.user
    playlistForm
      .bindFromRequest()(req, formBinding)
      .fold(
        errors =>
          service.playlistsMeta(user).map(pls => BadRequest(tags.playlists(pls.playlists, user))),
        submission => IO.pure(Redirect(routes.Playlists.playlists))
      )

  private def recoveredAsync(f: CookiedRequest[Unit, Username] => IO[Result]) =
    parsedRecoveredAsync(parsers.ignore(()))(req => f(req))

  private def parsedRecoveredAsync[T](
    parser: BodyParser[T]
  )(f: CookiedRequest[T, Username] => IO[Result]) =
    pimpParsedActionAsyncIO(parser)(auth => f(auth).recover(errorHandler))

  override def errorHandler: PartialFunction[Throwable, Result] =
    case ue: UnauthorizedException =>
      log.error(s"Unauthorized", ue)
      Errors.withStatus(Results.Unauthorized, "Access denied")
    case pe: PimpException =>
      log.error(s"Pimp error", pe)
      serverErrorGeneric
    case t: Throwable =>
      log.error(s"Server error", t)
      serverErrorGeneric
