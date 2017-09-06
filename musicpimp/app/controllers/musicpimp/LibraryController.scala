package controllers.musicpimp

import com.malliina.musicpimp.audio.TrackJson
import com.malliina.musicpimp.http.PimpContentController.default
import com.malliina.musicpimp.library.{Library, MusicFolder, MusicLibrary}
import com.malliina.musicpimp.models._
import com.malliina.musicpimp.tags.PimpHtml
import com.malliina.play.FileResults
import com.malliina.play.auth.AuthFailure
import com.malliina.play.models.Username
import com.malliina.play.tags.TagPage
import play.api.libs.json.Json
import play.api.mvc._

class LibraryController(tags: PimpHtml,
                        lib: MusicLibrary,
                        auth: AuthDeps)
  extends Secured(auth) {

  def siteRoot = rootLibrary

  def rootLibrary = pimpActionAsync { request =>
    lib.rootFolder.map(root => folderResult(root, request))
  }

  /**
    * @return an action that provides the contents of the library with the supplied id
    */
  def library(folderId: FolderID) = pimpActionAsync { request =>
    lib.folder(folderId).map { maybeFolder =>
      maybeFolder.map { items =>
        folderResult(items, request)
      }.getOrElse {
        folderNotFound(folderId, request)
      }
    }
  }

  def tracksIn(folderId: FolderID) = pimpActionAsync { request =>
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
    * @param trackId track to download
    */
  def download(trackId: TrackID): EssentialAction =
    customFailingPimpAction(onDownloadAuthFail) { authReq =>
      Library.findAbsolute(trackId)
        .map(path => FileResults.fileResult(path, authReq.rh, comps.fileMimeTypes))
        .getOrElse(NotFound(LibraryController.noTrackJson(trackId)))
    }

  def onDownloadAuthFail(failure: AuthFailure): Result = {
    Secured.logUnauthorized(failure.rh)
    Unauthorized
  }

  def meta(id: TrackID) = pimpAction { request =>
    implicit val writer = TrackJson.writer(request)
    Library.findMeta(id)
      .map(t => Ok(Json.toJson(t)))
      .getOrElse(trackNotFound(id))
  }

  private def trackNotFound(id: TrackID) = BadRequest(LibraryController.noTrackJson(id))

  def toHtml(folder: MusicFolder, username: Username): TagPage =
    tags.flexLibrary(folder, username)

  /** Arranges a music collection into columns.
    *
    * TODO: It could be interesting to explore a type like a non-empty list. Scalaz might have something.
    *
    * @param col      music collection
    * @param minCount minimum amount of items; if there are less items, only one column is used
    * @param columns  column count
    * @return at least one column
    */
  private def columnify(col: MusicFolder, minCount: Int = 20, columns: Int = 3): List[MusicColumn] = {
    val tracks = col.tracks
    val folders = col.folders
    val tracksCount = tracks.size
    val foldersCount = folders.size
    val itemsCount = tracksCount + foldersCount
    if (itemsCount < minCount || columns == 1) {
      List(MusicColumn(folders, tracks))
    } else {
      val cutoff = itemsCount / columns + 1
      val takeColumns = math.min(foldersCount, cutoff)
      val takeTracks = math.max(0, cutoff - foldersCount)
      val column = MusicColumn(folders take takeColumns, tracks take takeTracks)
      val remains = col.copy(folders = folders drop takeColumns, tracks = tracks drop takeTracks)
      column :: columnify(remains, 0, columns - 1)
    }
  }
}

object LibraryController {
  def noTrackJson(id: TrackID) = FailReason(s"Track not found: $id")
}
