package controllers

import java.nio.file.{Path, Paths}
import com.mle.musicpimp.library.{Folder, MusicCollection, Library}
import com.mle.musicpimp.json.JsonMessages
import com.mle.util.Log
import views.html
import play.api.templates.Html
import play.api.mvc._
import java.net.URLDecoder
import play.api.libs.json.Json

/**
 * @author Michael
 */
trait LibraryController extends Secured with Log {
  /**
   * @return an action that provides the contents of the library with the supplied id
   */
  def library(folderId: String) = PimpAction(implicit request => {
    val path = Library relativePath folderId
    Library.items(path).map(items => {
      respond(
        html = Website.display(folderId, path, items),
        json = Json.toJson(MusicCollection.fromFolder(folderId, path, items))
      )
    }).getOrElse {
      pimpResult(
        html = NotFound,
        json = NotFound(JsonMessages.failure(s"Unkown folder ID: $folderId"))
      )
    }
  })

  def rootLibrary = PimpAction(implicit request => {
    def items = Library.musicItems
    respond(
      html = Website.display("", Paths get "", items),
      json = Json.toJson(MusicCollection.fromFolder("", Paths get "", items))
    )
  })

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
  def download(trackId: String, f: SimpleResult => SimpleResult): EssentialAction =
    CustomFailingPimpAction(onDownloadAuthFail)(implicit req => {
      Library.toAbsolute(URLDecoder.decode(trackId, "UTF-8")).map(path => {
        f(Ok.sendFile(path.toFile))
      }).getOrElse(NotFound(JsonMessages.failure(s"Unable to find track with ID: $trackId")))
    })

  def onDownloadAuthFail(req: RequestHeader): SimpleResult = {
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
  def download(trackId: String): EssentialAction =
    download(trackId, _.withHeaders(ACCEPT_RANGES -> "bytes"))

  /**
   * Serves the given track but does NOT set the ACCEPT_RANGES header in the response.
   *
   * The Windows Phone background audio player fails to work properly if the
   * ACCEPT_RANGES header is set.
   *
   * @param trackId track to serve
   */
  def supplyForPlayback(trackId: String) =
    download(trackId, r => r)


  def display(id: String, relativePath: Path, contents: Folder): Html = {
    val itemsColl = MusicCollection.fromFolder(id, relativePath, contents)
    val allItems = itemsColl.dirs ++ itemsColl.songs
    val (col1, col2, col3) = columnize(allItems)
    html.library(relativePath, col1, col2, col3)
  }

  private def columnize[T](items: Seq[T]) = {
    val itemsCount = items.size
    if (itemsCount < 20) {
      (items, Nil, Nil)
    } else {
      val columnCount = 3
      val cutoff = itemsCount / columnCount
      val col1 = items.view(0, cutoff)
      val col2 = items.view(cutoff, cutoff * 2)
      val col3 = items.view(cutoff * 2, itemsCount - 1)
      (col1, col2, col3)
    }
  }

}
