package controllers.pimpcloud

import java.net.{URLDecoder, URLEncoder}
import java.nio.file.Paths

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.audio.Directory
import com.malliina.musicpimp.cloud.{PimpServerSocket, Search}
import com.malliina.musicpimp.models._
import com.malliina.musicpimp.stats.ItemLimits
import com.malliina.pimpcloud.auth.CloudAuthentication
import com.malliina.pimpcloud.json.JsonStrings._
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.pimpcloud.{ErrorMessage, ErrorResponse}
import com.malliina.play.ContentRange
import com.malliina.play.controllers.Caching
import com.malliina.play.http.HttpConstants
import controllers.pimpcloud.Phones.log
import play.api.Logger
import play.api.http.ContentTypes
import play.api.libs.MimeTypes
import play.api.libs.json.{JsObject, JsValue, Json, Writes}
import play.api.mvc._
import play.mvc.Http.HeaderNames

import scala.concurrent.Future
import scala.util.Try

object Phones {
  val log = Logger(getClass)

  val DefaultSearchLimit = 100
  val Bytes = "bytes"
  val EncodingUTF8 = "UTF-8"

  def invalidCredentials = new NoSuchElementException("Invalid credentials.")

  def path(id: TrackID) = Try(Paths get decode(id.id))

  def decode(id: String) = URLDecoder.decode(id, EncodingUTF8)

  def encode(id: String) = URLEncoder.encode(id, EncodingUTF8)
}

class Phones(tags: CloudTags,
             val cloudAuths: CloudAuthentication,
             val auth: CloudAuth)
  extends PimpContentController
    with Controller {

  def ping = proxiedGetAction(Ping)

  def pingAuth = proxiedAction { (_, socket) =>
    socket.server.pingAuth(socket.user).map(v => Caching.NoCacheOk(Json toJson v))
  }

  def rootFolder = folderAction(
    conn => conn.server.rootFolder(conn.user),
    conn => PhoneRequest(RootFolderKey, conn.user, Json.obj())
  )

  def folder(id: FolderID) = folderAction(
    conn => conn.server.folder(id, conn.user),
    conn => PhoneRequest(FolderKey, conn.user, WrappedID(id))
  )

  def status = proxiedGetAction(StatusKey)

  def search = proxiedAction { (req, socket) =>
    def query(key: String) = (req getQueryString key) map (_.trim) filter (_.nonEmpty)

    val termFromQuery = query(Term)
    val limit = query(Limit).filter(i => Try(i.toInt).isSuccess).map(_.toInt) getOrElse Phones.DefaultSearchLimit
    termFromQuery.fold[Future[Result]](fut(BadRequest)) { term =>
      folderResult(req, socket)(
        socket.server.search(term, socket.user, limit).map(tracks => Directory(Nil, tracks)),
        PhoneRequest(SearchKey, socket.user, Search(term, limit))
      )
    }
  }

  def alarms = proxiedGetAction(AlarmsKey)

  def editAlarm = bodyProxied(AlarmsEdit)

  def newAlarm = bodyProxied(AlarmsAdd)

  def beam = bodyProxied(Beam)

  /** Proxies track `id` from the desired target server to the requesting client.
    *
    * Sends a message over WebSocket to the target server that it should send `id` to this server.
    * This server then forwards the response of the target server to the client.
    *
    * @param id id of the requested track
    */
  def track(id: TrackID): EssentialAction = {
    phoneAction { conn =>
      val sourceServer = conn.server
      Action.async { req =>
        val userAgent = req.headers.get(HeaderNames.USER_AGENT) getOrElse "undefined"
        log info s"Serving track $id to user agent $userAgent"
        Phones.path(id).map { path =>
          val name = path.getFileName.toString
          // resolves track metadata from the server so we can set Content-Length
          log debug s"Looking up meta..."
          sourceServer.meta(id, conn.user).map { track =>
            // proxies request
            val trackSize = track.size
            val rangeTry = ContentRange.fromHeader(req, trackSize)
            val rangeOrAll = rangeTry getOrElse ContentRange.all(trackSize)
            val result = sourceServer.requestTrack(track, rangeOrAll, req)
            // ranged request support
            rangeTry map { range =>
              result.withHeaders(
                CONTENT_RANGE -> range.contentRange,
                CONTENT_LENGTH -> s"${range.contentLength}",
                CONTENT_TYPE -> MimeTypes.forFileName(name).getOrElse(ContentTypes.BINARY)
              )
            } getOrElse {
              result.withHeaders(
                ACCEPT_RANGES -> Phones.Bytes,
                CONTENT_LENGTH -> trackSize.toBytes.toString,
                CACHE_CONTROL -> HttpConstants.NoCache,
                CONTENT_TYPE -> HttpConstants.AudioMpeg,
                CONTENT_DISPOSITION -> s"""attachment; filename="$name""""
              )
            }
          }.recoverAll(_ => notFound(s"ID not found $id"))
        }.getOrElse(fut(badRequest(s"Illegal track ID $id")))
      }
    }
  }

  def playlists = proxiedGetAction(PlaylistsGet)

  def playlist(id: PlaylistID) = playlistAction(PlaylistGet, id)

  def savePlaylist = bodyProxied(PlaylistSave)

  def deletePlaylist(id: PlaylistID) = playlistAction(PlaylistDelete, id)

  def popular = metaAction(Popular)

  def recent = metaAction(Recent)

  def metaAction(cmd: String) =
    proxiedJsonAction(cmd) { req =>
      ItemLimits.fromRequest(req).right.flatMap { limits =>
        Json.toJson(limits).asOpt[JsObject].toRight("Not a JSON object")
      }
    }

  private def playlistAction(cmd: String, id: PlaylistID) =
    proxiedJsonAction(cmd)(_ => playlistIdJson(id))

  private def playlistIdJson(id: PlaylistID) = Right(WrappedLong(id.id))

  /** Sends the request body as JSON to the server this phone is connected to, and responds with the JSON the server
    * returned.
    *
    * The payload to the connected server will look like: { "cmd": "cmd_here", "body": "request_json_body_here" }
    *
    * @param cmd command to server
    * @return an action that responds as JSON with whatever the connected server returned in its `body` field
    */
  def bodyProxied(cmd: String) = customProxied(cmd) { req =>
    val body = req.body
    body.asOpt[JsObject].toRight(s"Body is not JSON object: '$body'.")
  }

  protected def customProxied(cmd: String)(body: Request[JsValue] => Either[String, JsObject]) =
    proxiedParsedJsonAction(parse.json)(cmd, body)

  private def folderAction[W: Writes](html: PhoneConnection => Future[Directory],
                                      json: PhoneConnection => PhoneRequest[W]) =
    proxiedAction((req, socket) => folderResult(req, socket)(html(socket), json(socket)))

  private def folderResult[W: Writes](req: RequestHeader,
                                      socket: PhoneConnection)(html: => Future[Directory],
                                                               json: => PhoneRequest[W]) =
    pimpResultAsync(req)(
      html.map(dir => Ok(tags.index(dir, None))),
      proxiedJson(json, socket)
    ).recoverAll(_ => BadGateway)

  private def proxiedAction(f: (Request[AnyContent], PhoneConnection) => Future[Result]): EssentialAction =
    proxiedParsedAction(parse.anyContent)(f)

  private def proxiedParsedAction[A](parser: BodyParser[A])(f: (Request[A], PhoneConnection) => Future[Result]): EssentialAction =
    phoneAction(socket => Action.async(parser)(req => f(req, socket)))

  private def proxiedGetAction(cmd: String) = proxiedJsonMessageAction(cmd)

  private def proxiedJsonMessageAction(cmd: String): EssentialAction =
    proxiedJsonAction(cmd)(_ => Right(Json.obj()))

  private def proxiedJsonAction[C: Writes](cmd: String)(f: RequestHeader => Either[String, C]): EssentialAction =
    proxiedParsedJsonAction(parse.anyContent)(cmd, f)

  private def proxiedParsedJsonAction[A, C: Writes](parser: BodyParser[A])(cmd: String, f: Request[A] => Either[String, C]): EssentialAction = {
    phoneAction { socket =>
      Action.async(parser) { req =>
        f(req).fold(
          err => fut(badRequest(err)),
          json => proxiedJson(PhoneRequest(cmd, socket.user, json), socket).recoverAll(_ => BadGateway))
      }
    }
  }

  def proxiedJson[W: Writes](req: PhoneRequest[W], conn: PhoneConnection): Future[Result] =
    conn.server.defaultProxy(req) map (js => Ok(js))

  def phoneAction(f: PhoneConnection => EssentialAction) =
    auth.loggedSecureActionAsync(rh => cloudAuths.authPhone(rh).flatMap(res => res.fold(_ => Future.failed(Phones.invalidCredentials), conn => fut(conn))))(f)

  def fut[T](body: => T) = Future successful body

  def notFound(message: String) = NotFound(simpleError(message))

  def badRequest(message: String) = BadRequest(simpleError(message))

  def serverError(message: String) = InternalServerError(simpleError(message))

  def simpleError(message: String) = ErrorResponse(Seq(ErrorMessage(message)))
}
