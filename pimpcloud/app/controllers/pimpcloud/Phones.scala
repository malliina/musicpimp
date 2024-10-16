package controllers.pimpcloud

import java.nio.file.Paths
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import com.malliina.concurrent.Execution.cached
import com.malliina.http.PlayCirce
import com.malliina.musicpimp.audio.{Directory, PimpEnc, Track}
import com.malliina.musicpimp.auth.PimpAuths
import com.malliina.musicpimp.cloud.{PimpServerSocket, Search}
import com.malliina.musicpimp.http.PimpContentController
import com.malliina.musicpimp.models.Errors.*
import com.malliina.musicpimp.models.*
import com.malliina.musicpimp.stats.ItemLimits
import com.malliina.pimpcloud.SharedStrings.Ping
import com.malliina.pimpcloud.json.JsonStrings.*
import com.malliina.pimpcloud.ws.PhoneConnection
import com.malliina.play.auth.Authenticator
import com.malliina.play.controllers.{BaseSecurity, Caching}
import com.malliina.web.HttpConstants
import com.malliina.play.tags.TagPage
import com.malliina.play.{ContentRange, ContentRanges}
import controllers.pimpcloud.Phones.log
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import io.circe.syntax.EncoderOps
import play.api.Logger
import play.api.http.Writeable
import play.api.mvc.*
import play.mvc.Http.HeaderNames

import scala.concurrent.Future
import scala.util.Try

object Phones:
  val log = Logger(getClass)

  val Bytes = "bytes"
  val DefaultSearchLimit = 100

  def forAuth(
    comps: ControllerComponents,
    tags: CloudTags,
    phonesAuth: Authenticator[PhoneConnection],
    mat: Materializer
  ): Phones =
    val bundle = PimpAuths.redirecting(routes.Web.login, phonesAuth)
    val phoneAuth = new BaseSecurity(comps.actionBuilder, bundle, mat)
    new Phones(comps, tags, phoneAuth)

  def invalidCredentials: NoSuchElementException = new NoSuchElementException(
    "Invalid credentials."
  )

class Phones(comps: ControllerComponents, tags: CloudTags, phoneAuth: BaseSecurity[PhoneConnection])
  extends AbstractController(comps)
  with PimpContentController
  with PlayCirce:

  def ping = proxiedGetAction(Ping)

  def pingAuth = executeProxied(parse.ignore(()))(VersionKey, _ => Right(Json.obj())): (_, json) =>
    json
      .as[Version]
      .fold(
        err => onGatewayParseErrorResult(err),
        v => Caching.NoCacheOk(v)
      )

  def status = proxiedGetAction(StatusKey)

  def rootFolder = executeFolderBasic(RootFolderKey, Json.obj())

  def folder(id: FolderID) = executeFolderBasic(FolderKey, WrappedID.forId(PimpEnc.folder(id)))

  def search = executeFolder(SearchKey, parseSearch)

  def parseSearch(req: RequestHeader): Either[String, Search] =
    def query(key: String) = (req getQueryString key)
      .map(_.trim)
      .filter(_.nonEmpty)

    val limit = query(Limit)
      .flatMap(i => Try(i.toInt).toOption)
      .getOrElse(Phones.DefaultSearchLimit)
    query(Term)
      .map(term => Search(term, limit))
      .toRight("Search term missing.")

  def alarms = proxiedGetAction(AlarmsKey)
  def editAlarm = bodyProxied(AlarmsEdit)
  def newAlarm = bodyProxied(AlarmsAdd)
  def beam = bodyProxied(Beam)

  def headTrack(t: TrackID): EssentialAction = withTrack(t): (track, _, _) =>
    // .withHeaders does not work for Content-Length; akka http ignores it
    Ok.streamed[ByteString](
      Source.empty,
      Option(track.size.toBytes),
      Option(HttpConstants.AudioMpeg)
    ).withHeaders(trackHeaders(name(track, t))*)

  /** Proxies track `id` from the desired target server to the requesting client.
    *
    * Sends a message over WebSocket to the target server that it should send `id` to this server.
    * This server then forwards the response of the target server to the client.
    *
    * @param in
    *   id of the requested track
    */
  def track(in: TrackID): EssentialAction =
    withTrack(in): (track, conn, req) =>
      val id = PimpEnc.track(in)
      val sourceServer: PimpServerSocket = conn.server
      val userAgent = req.headers.get(HeaderNames.USER_AGENT) getOrElse "undefined"
      log info s"Serving track '${track.title}' at '${track.path}' with ID '$id' to user agent '$userAgent'."
      // proxies request
      val trackSize = track.size
      val rangeTry = ContentRanges.fromHeader(req, trackSize)

      val rangeOrAll = rangeTry getOrElse ContentRange.all(trackSize)
      val result = sourceServer.requestTrack(track, rangeOrAll, req)
      // ranged request support
      val fileName = name(track, in)
      rangeTry
        .map: range =>
          result.withHeaders(CONTENT_RANGE -> range.contentRange)
        .getOrElse:
          result.withHeaders(trackHeaders(fileName)*)

  private def withTrack(
    in: TrackID
  )(code: (Track, PhoneConnection, Request[AnyContent]) => Result) =
    val id = PimpEnc.track(in)
    phoneAuth.authenticatedLogged: (conn: PhoneConnection) =>
      Action.async: req =>
        // resolves track metadata from the server so we can set Content-Length
        log.debug(s"Looking up meta...")
        conn
          .meta(id)
          .map[Result]: res =>
            res
              .map: track =>
                code(track, conn, req)
              .getOrElse:
                log.error(s"Found no info about track '$id', failing request.")
                badGatewayDefault
          .recover:
            case _ => notFound(s"ID not found '$id'.")

  def name(t: Track, in: TrackID) =
    Option(Paths.get(t.path.path).getFileName).map(_.toString).getOrElse(in.id)

  def trackHeaders(fileName: String) = Seq(
    ACCEPT_RANGES -> Phones.Bytes,
    CACHE_CONTROL -> HttpConstants.NoCache,
    CONTENT_DISPOSITION -> s"""inline; filename="$fileName""""
  )

  def playlists = proxiedGetAction(PlaylistsGet)

  def playlist(id: PlaylistID) = playlistAction(PlaylistGet, id)

  def savePlaylist = bodyProxied(PlaylistSave)

  def deletePlaylist(id: PlaylistID) = playlistAction(PlaylistDelete, id)

  def popular = metaAction(Popular)

  def recent = metaAction(Recent)

  def metaAction(cmd: String) =
    proxiedJsonAction(cmd): req =>
      ItemLimits
        .fromRequest(req)
        .map: limits =>
          limits.asJson

  private def playlistAction(cmd: String, id: PlaylistID) =
    proxiedJsonAction(cmd)(_ => playlistIdJson(id))

  private def playlistIdJson(id: PlaylistID) = Right(WrappedLong(id.id))

  /** Sends the request body as JSON to the server this phone is connected to, and responds with the
    * JSON the server returned.
    *
    * The payload to the connected server will look like: { "cmd": "cmd_here", "body":
    * "request_json_body_here" }
    *
    * @param cmd
    *   command to server
    * @return
    *   an action that responds as JSON with whatever the connected server returned in its `body`
    *   field
    */
  def bodyProxied(cmd: String) = customProxied(cmd): req =>
    val body = req.body
    Right(body)

  protected def customProxied(
    cmd: String
  )(body: Request[Json] => Either[String, Json]): EssentialAction =
    executeOkProxied(PlayCirce.circeParser(comps.parsers.json))(cmd, body)

  private def proxiedGetAction(cmd: String) = proxiedJsonMessageAction(cmd)

  private def proxiedJsonMessageAction(cmd: String): EssentialAction =
    proxiedJsonAction(cmd)(_ => Right(Json.obj()))

  private def proxiedJsonAction[C: Encoder](
    cmd: String
  )(f: RequestHeader => Either[String, C]): EssentialAction =
    executeOkProxied(parse.anyContent)(cmd, f)

  private def executeFolderBasic[W: Encoder](cmd: String, body: W) =
    executeFolder(cmd, _ => Right(body))

  private def executeFolder[W: Encoder](cmd: String, build: RequestHeader => Either[String, W]) =
    execute[W, TagPage](cmd, build): json =>
      json
        .as[Directory]
        .map: dir =>
          tags.index(dir, None)

  private def execute[W: Encoder, C: Writeable](
    cmd: String,
    build: RequestHeader => Either[String, W]
  )(toHtml: Json => Decoder.Result[C]) =
    executeProxied(parse.ignore(()))(cmd, build): (req, json) =>
      pimpResult(req)(
        html = toHtml(json).fold(
          err => onGatewayParseErrorResult(err),
          c => Ok(c)
        ),
        json = Ok(json)
      )

  private def executeOkProxied[A, W: Encoder](
    parser: BodyParser[A]
  )(cmd: String, build: Request[A] => Either[String, W]) =
    executeProxied(parser)(cmd, build)((_, json) => Ok(json))

  /** Parses a request from a phone, sends it to the server, and returns the server's response to
    * the phone.
    *
    * Provides the server's JSON response to the phone as-is.
    *
    * @param toResult
    *   Server's JSON response -> phone's response. The server JSON is returned as-is.
    * @tparam A
    *   request body
    * @tparam W
    *   message to send to server
    */
  private def executeProxied[A, W: Encoder](parser: BodyParser[A])(
    cmd: String,
    build: Request[A] => Either[String, W]
  )(toResult: (RequestHeader, Json) => Result): EssentialAction =
    proxiedParsedAction(parser): (req, phone) =>
      build(req).fold(
        errorMessage => fut(badRequest(errorMessage)),
        parsedBody =>
          phone
            .makeRequest(cmd, parsedBody)
            .map: response =>
              toResult(req, response)
            .recover:
              case t =>
                log.error("Phone request failed.", t)
                badGatewayDefault
      )

  private def proxiedParsedAction[A](
    parser: BodyParser[A]
  )(f: (Request[A], PhoneConnection) => Future[Result]): EssentialAction =
    phoneAuth.authenticatedLogged((socket: PhoneConnection) =>
      Action.async(parser)(req => f(req, socket))
    )

  private def onGatewayParseErrorResult(err: DecodingFailure) =
    log.error(s"Parse error. $err")
    badGateway("A dependent server returned unexpected data.")

  def fut[T](body: => T) = Future successful body
