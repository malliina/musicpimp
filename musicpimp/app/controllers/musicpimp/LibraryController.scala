package controllers.musicpimp

import com.malliina.musicpimp.audio.{PimpEnc, TrackJson}
import com.malliina.musicpimp.http.PimpContentController.default
import com.malliina.musicpimp.library.{Library, MusicFolder, MusicLibrary}
import com.malliina.musicpimp.models._
import com.malliina.musicpimp.tags.PimpHtml
import com.malliina.play.FileResults
import com.malliina.play.auth.AuthFailure
import com.malliina.play.models.Username
import com.malliina.play.tags.TagPage
import controllers.musicpimp.LibraryController.log
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._

object LibraryController {
  private val log = Logger(getClass)

  def noTrackJson(id: TrackID) = FailReason(s"Track not found: $id")
}

class LibraryController(tags: PimpHtml,
                        lib: MusicLibrary,
                        auth: AuthDeps)
  extends Secured(auth) {

  def siteRoot = rootLibrary

  def rootLibrary = pimpActionAsync { request =>
    lib.rootFolder.map { root =>
      folderResult(root, request)
    }
  }

  /**
    * @return an action that provides the contents of the library with the supplied id
    */
  def library(in: FolderID) = pimpActionAsync { request =>
    val folderId = PimpEnc.folder(in)
    lib.folder(folderId).map { maybeFolder =>
      maybeFolder.map { items =>
        folderResult(items, request)
      }.getOrElse {
        log.warn(s"Folder not found: '$folderId'.")
        folderNotFound(folderId, request)
      }
    }
  }

  def tracksIn(in: FolderID) = pimpActionAsync { request =>
    val folderId = PimpEnc.folder(in)
    implicit val writer = TrackJson.writer(request)
    lib.tracksIn(folderId).map { maybeTracks =>
      maybeTracks.map { tracks =>
        Ok(Json.toJson(tracks))
      }.getOrElse {
        folderNotFound(folderId, request)
      }
    }
  }

  def allTracks = tracksIn(Library.RootId)

  private def folderNotFound(id: FolderID, request: RequestHeader): Result = {
    default.pimpResult(request)(
      html = NotFound,
      json = notFound(s"Folder not found: $id")
    )
  }

  private def folderResult(collection: => MusicFolder, request: PimpUserRequest): Result = {
    default.respond(request)(
      html = toHtml(collection, request.user),
      json = Json.toJson(collection)(MusicFolder.writer(request))
    )
  }

  /** Legacy.
    *
    * @param trackId track to serve
    */
  def supplyForPlayback(trackId: TrackID) = download(trackId)

  /** Responds with the song with the given ID.
    *
    * Note: If an unauthorized request is made here, the result is always
    * Unauthorized with JSON content. This differs from the default of
    * redirecting to the login page if the client accepts HTML, because
    * the Background Transfer Service in WP8 makes download requests
    * accepting any response format, yet we want to respond with an
    * Unauthorized as opposed to a redirect to make it easier to deal
    * with download errors on the client side.
    *
    * @param in track to download
    */
  def download(in: TrackID): EssentialAction =
    customFailingPimpAction(onDownloadAuthFail) { authReq =>
      val trackId = PimpEnc.track(in)
      Library.findAbsolute(trackId)
        .map(path => FileResults.fileResult(path, authReq.rh, comps.fileMimeTypes))
        .getOrElse(NotFound(LibraryController.noTrackJson(trackId)))
    }

  def onDownloadAuthFail(failure: AuthFailure): Result = {
    Secured.logUnauthorized(failure.rh)
    Unauthorized
  }

  def meta(id: TrackID) = pimpAction { request =>
    val trackId = PimpEnc.track(id)
    implicit val writer = TrackJson.writer(request)
    Library.findMeta(PimpEnc.track(trackId))
      .map(t => Ok(Json.toJson(t)))
      .getOrElse(trackNotFound(trackId))
  }

  private def trackNotFound(id: TrackID) = BadRequest(LibraryController.noTrackJson(id))

  def toHtml(folder: MusicFolder, username: Username): TagPage =
    tags.flexLibrary(folder, username)
}
