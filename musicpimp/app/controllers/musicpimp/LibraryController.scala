package controllers.musicpimp

import cats.effect.IO
import com.malliina.concurrent.Execution.runtime
import com.malliina.musicpimp.audio.{PimpEnc, TrackJson, TrackMeta}
import com.malliina.musicpimp.html.PimpHtml
import com.malliina.musicpimp.http.PimpContentController.default
import com.malliina.musicpimp.json.JsonMessages
import com.malliina.musicpimp.library.{Library, MusicFolder, MusicLibrary}
import com.malliina.musicpimp.models.*
import com.malliina.play.FileResults
import com.malliina.play.auth.AuthFailure
import com.malliina.play.http.FullUrls
import com.malliina.values.Username
import com.malliina.play.tags.TagPage
import controllers.musicpimp.LibraryController.log
import io.circe.Encoder
import play.api.Logger
import play.api.mvc.*

object LibraryController:
  private val log = Logger(getClass)

  def noTrackJson(id: TrackID) = FailReason(s"Track not found: $id")

class LibraryController(tags: PimpHtml, lib: MusicLibrary[IO], auth: AuthDeps)
  extends Secured(auth):

  def siteRoot = rootLibrary

  def rootLibrary = pimpActionAsyncIO: request =>
    lib.rootFolder.map: root =>
      folderResult(root, request)

  /** @return
    *   an action that provides the contents of the library with the supplied id
    */
  def library(in: FolderID) = pimpActionAsyncIO: request =>
    val folderId = PimpEnc.folder(in)
    lib
      .folder(folderId)
      .map: maybeFolder =>
        maybeFolder
          .map: items =>
            folderResult(items, request)
          .getOrElse:
            log.warn(s"Folder not found: '$folderId'.")
            folderNotFound(folderId, request)

  def tracksIn(in: FolderID) = pimpActionAsyncIO: request =>
    val folderId = PimpEnc.folder(in)
    given Encoder[TrackMeta] = TrackJson.writer(request)
    given Encoder[List[TrackMeta]] = Encoder.encodeList[TrackMeta]
    lib
      .tracksIn(folderId)
      .map: maybeTracks =>
        maybeTracks
          .map: tracks =>
            Ok(tracks)
          .getOrElse:
            folderNotFound(folderId, request)

  def allTracks = tracksIn(Library.RootId)

  private def folderNotFound(id: FolderID, request: RequestHeader): Result =
    default.pimpResult(request)(
      html = NotFound,
      json = notFound(s"Folder not found: $id")
    )

  private def folderResult(collection: => MusicFolder, request: PimpUserRequest): Result =
    given Encoder[MusicFolder] = MusicFolder.writer(request)
    default.respond(request)(
      html = toHtml(collection, request.user),
      json = collection
    )

  /** Responds with the song with the given ID.
    *
    * Note: If an unauthorized request is made here, the result is always Unauthorized with JSON
    * content. This differs from the default of redirecting to the login page if the client accepts
    * HTML, because the Background Transfer Service in WP8 makes download requests accepting any
    * response format, yet we want to respond with an Unauthorized as opposed to a redirect to make
    * it easier to deal with download errors on the client side.
    *
    * @param id
    *   track to download
    */
  def download(id: TrackID): EssentialAction =
    customFailingPimpAction(onDownloadAuthFail): authReq =>
      lib
        .findFile(id)
        .unsafeToFuture()
        .map: maybeTrack =>
          maybeTrack
            .map(path => FileResults.fileResult(path, authReq.rh, comps.fileMimeTypes))
            .getOrElse(NotFound(LibraryController.noTrackJson(id)))
        .recover:
          case e: Exception =>
            log.error("Track error.", e)
            Results.InternalServerError(JsonMessages.databaseFailure)

  def onDownloadAuthFail(failure: AuthFailure): Result =
    Secured.logUnauthorized(failure.rh)
    Unauthorized

  def meta(id: TrackID) = pimpActionAsyncIO: request =>
    lib
      .track(id)
      .map: maybeTrack =>
        maybeTrack
          .map(t => Ok(TrackJson.toFull(t, FullUrls.hostOnly(request))))
          .getOrElse(trackNotFound(id))

  private def trackNotFound(id: TrackID) = BadRequest(LibraryController.noTrackJson(id))

  def toHtml(folder: MusicFolder, username: Username): TagPage =
    tags.flexLibrary(folder, username)
