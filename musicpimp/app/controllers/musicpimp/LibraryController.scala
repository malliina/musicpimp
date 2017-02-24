package controllers.musicpimp

import akka.stream.Materializer
import com.malliina.musicpimp.audio.TrackJson
import com.malliina.musicpimp.json.JsonMessages
import com.malliina.musicpimp.library.{Library, MusicFolder, MusicLibrary}
import com.malliina.musicpimp.models.{FolderID, MusicColumn, TrackID}
import com.malliina.musicpimp.tags.{PimpTags, TagPage}
import com.malliina.play.models.Username
import com.malliina.play.{CookieAuthenticator, FileResults}
import play.api.libs.json.Json
import play.api.mvc._

class LibraryController(tags: PimpTags, lib: MusicLibrary, auth: CookieAuthenticator, mat: Materializer)
  extends Secured(auth, mat) {

  def siteRoot = rootLibrary

  def rootLibrary = pimpActionAsync { request =>
    lib.rootFolder.map(root => folderResult(root, request))
  }

  /**
    * @return an action that provides the contents of the library with the supplied id
    */
  def library(folderId: FolderID) = pimpActionAsync { request =>
    lib.folder(folderId).map(_.fold(folderNotFound(folderId, request))(items => folderResult(items, request)))
  }

  def tracksIn(folderID: FolderID) = pimpActionAsync { request =>
    implicit val writer = TrackJson.writer(request)
    lib.tracksIn(folderID).map(_.fold(folderNotFound(folderID, request))(ts => Ok(Json.toJson(ts))))
  }

  def allTracks = tracksIn(Library.RootId)

  private def folderNotFound(id: FolderID, request: RequestHeader): Result = {
    pimpResult(request)(
      html = NotFound,
      json = notFound(s"Folder not found: $id")
    )
  }

  private def folderResult(collection: => MusicFolder, request: PimpRequest): Result = {
    respond(request)(
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
        .map(path => FileResults.fileResult(path, authReq.request))
        .getOrElse(NotFound(LibraryController.noTrackJson(trackId)))
    }

  def onDownloadAuthFail(req: RequestHeader): Result = {
    logUnauthorized(req)
    Unauthorized
  }

  def meta(id: TrackID) = pimpAction { request =>
    implicit val writer = TrackJson.writer(request)
    val metaResult = Library.findMeta(id).map(t => Json.toJson(t))
    metaResult.fold(trackNotFound(id))(json => Ok(json))
  }

  private def trackNotFound(id: TrackID) = BadRequest(LibraryController.noTrackJson(id))

  def toHtml(folder: MusicFolder, username: Username): TagPage = {
    val (col1, col2, col3) = columnify(folder) match {
      case Nil => (MusicColumn.empty, MusicColumn.empty, MusicColumn.empty)
      case h :: Nil => (h, MusicColumn.empty, MusicColumn.empty)
      case f :: s :: Nil => (f, s, MusicColumn.empty)
      case f :: s :: t :: tail => (f, s, t)
    }
    tags.library(folder.folder.path, col1, col2, col3, username)
  }

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
  def noTrackJson(id: TrackID) = JsonMessages.failure(s"Track not found: $id")
}
