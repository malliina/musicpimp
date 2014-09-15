package controllers

import java.net.URLDecoder
import java.nio.file.Paths

import com.mle.musicpimp.json.JsonMessages
import com.mle.musicpimp.library.{Library, MusicFolder}
import com.mle.util.Log
import models.MusicColumn
import play.api.libs.json.Json
import play.api.mvc._
import views.html

/**
 * @author Michael
 */
trait LibraryController extends Secured with Log {
  def rootLibrary = PimpAction(implicit request => folderResult(Library.rootFolder))

  /**
   * @return an action that provides the contents of the library with the supplied id
   */
  def library(folderId: String) = PimpAction(implicit request => {
    Library.folder(folderId).fold(folderNotFound(folderId))(items => folderResult(items))
  })

  private def folderNotFound(id: String)(implicit request: RequestHeader): Result = pimpResult(
    html = NotFound,
    json = NotFound(JsonMessages.failure(s"Unknown folder ID: $id"))
  )

  private def folderResult(collection: => MusicFolder)(implicit request: RequestHeader): Result = {
    respond(
      html = Website.toHtml(collection),
      json = Json.toJson(collection)
    )
  }

  /**
   * Responds with the song with the given ID.
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
  def download(trackId: String, f: Result => Result): EssentialAction =
    CustomFailingPimpAction(onDownloadAuthFail)(implicit req => {
      Library.findAbsolute(URLDecoder.decode(trackId, "UTF-8"))
        .fold(NotFound(JsonMessages.failure(s"Unable to find track with ID: $trackId")))(path => {
        f(Ok.sendFile(path.toFile))
      })
    })

  def onDownloadAuthFail(req: RequestHeader): Result = {
    logUnauthorized(req)
    Unauthorized
  }

  /**
   * Serves the given track and sets the ACCEPT_RANGES header in the response.
   *
   * The Windows Phone background downloader requires the accept-ranges
   * header for files over 5 MB.
   *
   * @param trackId track to serve
   */
  def download(trackId: String): EssentialAction = download(trackId, _.withHeaders(ACCEPT_RANGES -> "bytes"))

  /**
   * Serves the given track but does NOT set the ACCEPT_RANGES header in the response.
   *
   * The Windows Phone background audio player fails to work properly if the
   * ACCEPT_RANGES header is set.
   *
   * @param trackId track to serve
   */
  def supplyForPlayback(trackId: String) = download(trackId, r => r)

  def meta(id: String) = PimpAction {
    Library.findMeta(id).fold(trackNotFound(id))(track => Ok(Json.toJson(track)))
  }

  private def trackNotFound(id:String)=BadRequest(JsonMessages.failure(s"Unable to find track with ID: $id"))

  def toHtml(contents: MusicFolder): play.twirl.api.Html = {
    val (col1, col2, col3) = columnify(contents) match {
      case Nil => (MusicColumn.empty, MusicColumn.empty, MusicColumn.empty)
      case h :: Nil => (h, MusicColumn.empty, MusicColumn.empty)
      case f :: s :: Nil => (f, s, MusicColumn.empty)
      case f :: s :: t :: tail => (f, s, t)
    }
    html.library(Paths get contents.folder.path, col1, col2, col3)
  }

  /**
   *
   * @param col
   * @param minCount
   * @param columns
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
      column :: columnify(remains, minCount, columns - 1)
    }
  }
}
