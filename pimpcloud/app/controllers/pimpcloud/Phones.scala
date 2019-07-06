package controllers.pimpcloud

import java.nio.file.Paths

import akka.stream.Materializer
import com.malliina.concurrent.Execution.cached
import com.malliina.musicpimp.audio.{Directory, PimpEnc}
import com.malliina.musicpimp.auth.PimpAuths
import com.malliina.musicpimp.cloud.{PimpServerSocket, Search}
import com.malliina.musicpimp.http.PimpContentController
import com.malliina.musicpimp.models.Errors._
import com.malliina.musicpimp.models._
import com.malliina.musicpimp.stats.ItemLimits
import com.malliina.pimpcloud.json.JsonStrings._
import com.malliina.pimpcloud.ws.PhoneConnection
import com.malliina.play.auth.Authenticator
import com.malliina.play.controllers.{BaseSecurity, Caching}
import com.malliina.play.http.HttpConstants
import com.malliina.play.tags.TagPage
import com.malliina.play.{ContentRange, ContentRanges}
import controllers.pimpcloud.Phones.log
import play.api.Logger
import play.api.http.{ContentTypes, Writeable}
import play.api.libs.json._
import play.api.mvc._
import play.mvc.Http.HeaderNames

import scala.concurrent.Future
import scala.util.Try

object Phones {
  val log = Logger(getClass)

  val Bytes = "bytes"
  val DefaultSearchLimit = 100

  def forAuth(comps: ControllerComponents,
              tags: CloudTags,
              phonesAuth: Authenticator[PhoneConnection],
              mat: Materializer): Phones = {
    val bundle = PimpAuths.redirecting(routes.Web.login(), phonesAuth)
    val phoneAuth = new BaseSecurity(comps.actionBuilder, bundle, mat)
    new Phones(comps, tags, phoneAuth)
  }

  def invalidCredentials: NoSuchElementException = new NoSuchElementException("Invalid credentials.")
}

class Phones(comps: ControllerComponents,
             tags: CloudTags,
             phoneAuth: BaseSecurity[PhoneConnection])
  extends AbstractController(comps)
    with PimpContentController {

  def ping = proxiedGetAction(Ping)

  def pingAuth = executeProxied(parse.anyContent)(VersionKey, _ => Right(Json.obj())) { (_, json) =>
    json.validate[Version].fold(
      err => onGatewayParseErrorResult(err),
      v => Caching.NoCacheOk(v)
    )
  }

  def status = proxiedGetAction(StatusKey)

  def rootFolder = executeFolderBasic(RootFolderKey, Json.obj())

  def folder(id: FolderID) = executeFolderBasic(FolderKey, WrappedID.forId(PimpEnc.folder(id)))

  def search = executeFolder(SearchKey, parseSearch)

  def parseSearch(req: RequestHeader): Either[String, Search] = {
    def query(key: String) = (req getQueryString key)
      .map(_.trim).filter(_.nonEmpty)

    val limit = query(Limit)
      .flatMap(i => Try(i.toInt).toOption)
      .getOrElse(Phones.DefaultSearchLimit)
    query(Term)
      .map(term => Search(term, limit))
      .toRight("Search term missing.")
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
    * @param in id of the requested track
    */
  def track(in: TrackID): EssentialAction = {
    val id = PimpEnc.track(in)
    phoneAuth.authenticatedLogged { (conn: PhoneConnection) =>
      val sourceServer: PimpServerSocket = conn.server
      Action.async { req =>
        val userAgent = req.headers.get(HeaderNames.USER_AGENT) getOrElse "undefined"
        // resolves track metadata from the server so we can set Content-Length
        log debug s"Looking up meta..."
        conn.meta(id).map[Result] { res =>
          res.map { track =>
            log info s"Serving track '${track.title}' at '${track.path}' with ID '$id' to user agent '$userAgent'."
            // proxies request
            val trackSize = track.size
            val rangeTry = ContentRanges.fromHeader(req, trackSize)
            val rangeOrAll = rangeTry getOrElse ContentRange.all(trackSize)
            val result = sourceServer.requestTrack(track, rangeOrAll, req)
            // ranged request support
            val fileName = Option(Paths.get(track.path.path).getFileName).map(_.toString).getOrElse(in.id)
            rangeTry.map { range =>
              result.withHeaders(
                CONTENT_RANGE -> range.contentRange,
                CONTENT_LENGTH -> s"${range.contentLength}",
                CONTENT_TYPE -> fileMimeTypes.forFileName(fileName).getOrElse(ContentTypes.BINARY)
              )
            }.getOrElse {
              result.withHeaders(
                ACCEPT_RANGES -> Phones.Bytes,
                CACHE_CONTROL -> HttpConstants.NoCache,
                CONTENT_LENGTH -> trackSize.toBytes.toString,
                CONTENT_DISPOSITION -> s"""attachment; filename="$fileName"""",
                CONTENT_TYPE -> HttpConstants.AudioMpeg
              )
            }
          }.getOrElse {
            log.error(s"Found no info about track '$id', failing request.")
            badGatewayDefault
          }
        }.recover { case _ => notFound(s"ID not found '$id'.") }
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
      ItemLimits.fromRequest(req).flatMap { limits =>
        Json.toJson(limits).asOpt[JsObject].toRight("Not a JSON object.")
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

  protected def customProxied(cmd: String)(body: Request[JsValue] => Either[String, JsObject]): EssentialAction =
    executeOkProxied(parse.json)(cmd, body)

  private def proxiedGetAction(cmd: String) = proxiedJsonMessageAction(cmd)

  private def proxiedJsonMessageAction(cmd: String): EssentialAction =
    proxiedJsonAction(cmd)(_ => Right(Json.obj()))

  private def proxiedJsonAction[C: Writes](cmd: String)(f: RequestHeader => Either[String, C]): EssentialAction =
    executeOkProxied(parse.anyContent)(cmd, f)

  private def executeFolderBasic[W: Writes](cmd: String, body: W) =
    executeFolder(cmd, _ => Right(body))

  private def executeFolder[W: Writes](cmd: String, build: RequestHeader => Either[String, W]) =
    execute[W, TagPage](cmd, build) { json =>
      json.validate[Directory].map { dir =>
        tags.index(dir, None)
      }
    }

  private def execute[W: Writes, C: Writeable](cmd: String, build: RequestHeader => Either[String, W])(toHtml: JsValue => JsResult[C]) =
    executeProxied(parse.anyContent)(cmd, build) { (req, json) =>
      pimpResult(req)(
        html = toHtml(json).fold(
          err => onGatewayParseErrorResult(err),
          c => Ok(c)
        ),
        json = Ok(json)
      )
    }

  private def executeOkProxied[A, W: Writes](parser: BodyParser[A])(cmd: String, build: Request[A] => Either[String, W]) =
    executeProxied(parser)(cmd, build)((_, json) => Ok(json))

  private def executeProxied[A, W: Writes](parser: BodyParser[A])(cmd: String,
                                                                  build: Request[A] => Either[String, W])(toResult: (RequestHeader, JsValue) => Result) =
    proxiedParsedAction(parser) { (req, phone) =>
      build(req).fold(
        errorMessage => fut(badRequest(errorMessage)),
        parsedBody => phone.makeRequest(cmd, parsedBody)
          .map { response => toResult(req, response) }
          .recover { case t =>
            log.error("Phone request failed.", t)
            badGatewayDefault
          }
      )
    }

  private def proxiedParsedAction[A](parser: BodyParser[A])(f: (Request[A], PhoneConnection) => Future[Result]): EssentialAction =
    phoneAuth.authenticatedLogged((socket: PhoneConnection) => Action.async(parser)(req => f(req, socket)))

  private def onGatewayParseErrorResult(err: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]) = {
    log.error(s"Parse error. $err")
    badGateway("A dependent server returned unexpected data.")
  }

  def fut[T](body: => T) = Future successful body
}
