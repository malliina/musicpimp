package com.malliina.http

import java.io.Closeable
import java.nio.file.{Files, Path}

import com.malliina.concurrent.Execution
import com.malliina.http.DiscoClient.{keys, log}
import com.malliina.oauth.DiscoGsOAuthCredentials
import com.malliina.storage._
import org.apache.commons.codec.digest.DigestUtils
import play.api.Logger
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

object DiscoClient {
  private val log = Logger(getClass)

  object keys {
    val CoverImage = "cover_image"
    val Id = "id"
    val Images = "images"
    val Results = "results"
    val Uri = "uri"
  }

  def apply(creds: DiscoGsOAuthCredentials, coverDir: Path): DiscoClient =
    new DiscoClient(creds, coverDir)(Execution.cached)
}

class DiscoClient(credentials: DiscoGsOAuthCredentials, coverDir: Path)(implicit ec: ExecutionContext) extends Closeable {
  Files.createDirectories(coverDir)
  val httpClient = OkClient.default
  val consumerKey = credentials.consumerKey
  val consumerSecret = credentials.consumerSecret
  val iLoveDiscoGsFakeCoverSize = 15378

  /** Returns the album cover. Optionally downloads and caches it if it doesn't already exist locally.
    *
    * Fails with a [[NoSuchElementException]] if the cover cannot be found. Can also fail with a [[java.io.IOException]]
    * and a [[com.fasterxml.jackson.core.JsonParseException]].
    *
    * @return the album cover file, which is an image
    */
  def cover(artist: String, album: String): Future[Path] = {
    val file = coverFile(artist, album)
    if (Files.isReadable(file) && Files.size(file) != iLoveDiscoGsFakeCoverSize) Future.successful(file)
    else downloadCover(artist, album).filter(f => Files.size(f) != iLoveDiscoGsFakeCoverSize)
  }

  def downloadCover(artist: String, album: String): Future[Path] =
    downloadCover(artist, album, _ => coverFile(artist, album))

  /** Streams `url` to `file`.
    *
    * @param url  url to download
    * @param file destination path
    * @return the size of the downloaded file, stored in `file`
    * @see http://www.playframework.com/documentation/2.6.x/ScalaWS
    */
  protected def downloadFile(url: FullUrl, file: Path): Future[StorageSize] = {
    httpClient.download(url, file, Map(AUTHORIZATION -> authValue)).flatMap { either =>
      either.fold(
        err => Future.failed(new ResponseException(err.response, url)),
        s => Future.successful(s)
      )
    }
  }

  protected def coverFile(artist: String, album: String): Path = {
    // avoids platform-specific file system encoding nonsense
    val hash = DigestUtils.md5Hex(s"$artist-$album")
    coverDir resolve s"$hash.jpg"
  }

  /** Downloads the album cover of `artist`s `album`.
    *
    * Performs three web requests in sequence to the DiscoGs API:
    *
    * 1) Obtains the album ID
    * 2) Obtains the album details (with the given album ID)
    * 3) Downloads the album cover (the URL of which is available in the details)
    *
    * At least the last step, which downloads the cover, requires OAuth authentication.
    *
    * @param artist  the artist
    * @param album   the album
    * @param fileFor the file to download the cover to, given its remote URL
    * @return the downloaded album cover along with the number of bytes downloaded
    */
  protected def downloadCover(artist: String, album: String, fileFor: FullUrl => Path): Future[Path] =
    for {
      url <- albumCoverForSearch(albumIdUrl(artist, album))
      file = fileFor(url)
      _ <- downloadFile(url, file)
    } yield file

  private def albumCoverForSearch(url: FullUrl): Future[FullUrl] =
    getResponse(url).map { r =>
      coverImageForResult(Json.parse(r.asString))
        .getOrElse(throw new CoverNotFoundException(s"Unable to find cover image from response: '${r.asString}'."))
    }

  private def getResponse(url: FullUrl): Future[HttpResponse] = authenticated(url)
    .flatMap(r => validate(r, url).fold(Future.successful(r))(Future.failed))

  private def authenticated(url: FullUrl) = {
    log debug s"Preparing authenticated request to '$url'..."
    httpClient.get(url, Map(AUTHORIZATION -> authValue))
  }

  private def albumIdUrl(artist: String, album: String): FullUrl = {
    val artistEnc = WebUtils.encodeURIComponent(artist)
    val albumEnc = WebUtils.encodeURIComponent(album)
    FullUrl.https("api.discogs.com", s"/database/search?artist=$artistEnc&release_title=$albumEnc")
  }

  private def validate(wsResponse: HttpResponse, url: FullUrl): Option[Exception] = {
    val code = wsResponse.code
    code match {
      case c if (c >= 200 && c < 300) || c == 404 => None
      case _ => Option(new ResponseException(wsResponse, url))
    }
  }

  private def coverImageForResult(json: JsValue): Option[FullUrl] = {
    (json \ keys.Results \\ keys.CoverImage).headOption.flatMap(_.asOpt[FullUrl])
  }

  private def authValue = s"Discogs key=$consumerKey, secret=$consumerSecret"

  def close(): Unit = httpClient.close()
}
