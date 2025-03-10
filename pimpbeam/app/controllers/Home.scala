package controllers

import java.util.UUID
import org.apache.pekko.stream.QueueOfferResult.{Dropped, Enqueued, Failure, QueueClosed}
import org.apache.pekko.stream.{Materializer, QueueOfferResult}
import org.apache.pekko.util.ByteString
import com.malliina.beam.*
import com.malliina.http.PlayCirce
import com.malliina.play.BeamStrings.*
import com.malliina.play.auth.Authenticator.Outcome
import com.malliina.play.auth.{Auth, AuthFailure, BasicCredentials, InvalidCredentials}
import com.malliina.play.controllers.Caching
import com.malliina.play.json.JsonMessages
import com.malliina.play.streams.StreamParsers
import com.malliina.storage.StorageInt
import com.malliina.values.{Password, Username}
import com.malliina.web.HttpConstants.{AudioMpeg, NoCache}
import controllers.Home.log
import io.circe.Json
import io.circe.syntax.EncoderOps
import net.glxn.qrgen.QRCode
import play.api.Logger
import play.api.http.{HeaderNames, HttpEntity, HttpErrorHandler, MimeTypes}
import play.api.libs.streams.Accumulator
import play.api.libs.ws.WSRequest
import play.api.mvc.*

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try

/** @see
  *   http://greweb.me/2012/08/zound-a-playframework-2-audio-streaming-experiment-using-iteratees/
  */
object Home:
  private val log = Logger(getClass)

  // reads settings
  //  lazy val isHttpAvailable = AppConf.isHttpAvailable
  //  lazy val isHttpsAvailable = AppConf.isHttpsAvailable

  def validateCredentials(creds: BasicCredentials): Boolean =
    val user = creds.username
    // the password is not really a secret
    val credsOk = user.name.nonEmpty && creds.password == Password("beam")
    if !credsOk then log warn s"Invalid credentials provided as user '$user'."
    credsOk

class Home(
  beamConf: BeamConf,
  beams: Beams,
  disco: DiscoGs,
  mat: Materializer,
  errorHandler: HttpErrorHandler,
  comps: ControllerComponents
) extends AbstractController(comps)
  with PlayCirce:
  implicit val m: Materializer = mat
  implicit val ec: ExecutionContextExecutor = mat.executionContext
  val sessionKey = "username"
  val maxSize = 1024.megs

  def ping = Action(Caching.NoCacheOk(BuildMeta.default.asJson))

  // Player actions

  /** The landing page of the browser, after which image is called, after which stream is called by
    * the browser if a mobile device has happened to connect and started streaming something.
    */
  def index = Action: (request: Request[AnyContent]) =>
    val user = UUID.randomUUID().toString
    val remoteIP = request.remoteAddress
    log info s"Created user '$user' from '$remoteIP'."
    Ok(BeamTags.index).withSession(sessionKey -> user)

  /** Not sure if the websocket connection has been opened by the time the call to this resource is
    * made, hence the "Limited" security check, but it matters little.
    */
  def image = PlayerLimitedSecureAction: user =>
    Action:
      val qrText = coordinate(user).asJson.noSpaces
      log info s"Generating image with QR code: $qrText"
      val qrFile = QRCode.from(qrText).withSize(768, 768).file()
      Caching.NoCache(Ok.sendFile(qrFile))

  /** Loads an album cover from DiscoGs into memory, then sends it to the client.
    *
    * Set the route that calls this action as the `src` attribute of an `img` element to display the
    * cover.
    *
    * @return
    *   an action that returns the cover of `artist` s `album`
    */
  def cover(artist: String, album: String) = PlayerSecureAction: player =>
    log info s"Searching for cover: $artist - $album"
    Action.async:
      streamedRequest(disco.request(artist, album), _ => defaultCover)

  /** Performs a web request as configured by `requestHolder` and returns a stream of the response
    * body as a [[Result]].
    *
    * However, if the request returns with a non-200 status code, `onError` is run and returned.
    *
    * @param requestHolder
    *   request to execute
    * @param onError
    *   error handler
    * @return
    *   the result
    * @see
    *   https://www.playframework.com/documentation/2.3.x/ScalaWS
    */
  def streamedRequest(requestHolder: WSRequest, onError: Int => Result = Status): Future[Result] =
    requestHolder
      .stream()
      .map: response =>
        def header(name: String) = response.headers.get(name).flatMap(_.headOption)

        val statusCode = response.status
        if statusCode == play.api.http.Status.OK then
          val contentType = header(HeaderNames.CONTENT_TYPE) getOrElse MimeTypes.BINARY
          val contentLength =
            header(HeaderNames.CONTENT_LENGTH).flatMap(s => Try(s.toLong).toOption)
          val entity =
            HttpEntity.Streamed(response.bodyAsSource, contentLength, Option(contentType))
          Ok.sendEntity(entity)
        else onError(statusCode)

  def defaultCover = Redirect(controllers.routes.Assets.versioned("img/guitar.png"))

  private def coordinate(user: Username): Json = Json.obj(
    BEAM_HOST -> beamConf.host.asJson,
    PORT -> beamConf.port.asJson,
    SSL_PORT -> beamConf.sslPort.asJson,
    USER -> user.name.asJson,
    SUPPORTS_PLAINTEXT -> false.asJson,
    SUPPORTS_TLS -> true.asJson
  )

  /** Opens a connection to the client by returning a chunked HTTP response. This connection streams
    * music to the client.
    *
    * The browser sets the audio source to "stream" only after being directed by the server;
    * therefore we can require it to be authenticated by this time, as the reception of a message
    * already implies it is connected and authenticated.
    */
  def stream = PlayerSecureAction: player =>
    Action: req =>
      log.info(s"Sending stream to player '${player.user}'...")
      Ok.chunked(player.stream)
        .as(AudioMpeg)
        .withHeaders(CACHE_CONTROL -> NoCache)

  // Phone actions

  def playerState = PhoneLimitedSecureAction: user =>
    Action.async: _ =>
      beams
        .findPlayer(user)
        .map: maybePlayer =>
          val json = BeamMessages.playerExists(
            user,
            maybePlayer.isDefined,
            maybePlayer.exists(p => !p.streamer.isReceivingStream)
          )
          Caching.NoCacheOk(json)

  /** Replaces the playlist with the uploaded file.
    */
  def resetAndReceiveFile = PhoneSecureAction: player =>
    player.resetStream()
    tryPushFile(player)

  /** Adds the uploaded file to the playlist.
    */
  def receiveFile = PhoneSecureAction(tryPushFile)

  private def tryPushFile(player: PlayerClient): EssentialAction =
    val user = player.user
    val streamer = player.streamer
    if streamer.isReceivingStream then
      // Prevents two songs from playing at the same time in the player
      // A consequence of this implementation is that adding a track to
      // the playlist while a previous track is still being uploaded
      // will fail
      // TODO play with enumerators etc and see if this can be fixed
      streamRefusalResponse(user)
    else serveFile(user, streamer)

  private def serveFile(user: Username, streamer: StreamManager): EssentialAction =
    log.info(s"Serving file to user '$user'...")
    streamer.isReceivingStream = true
    // the body parser pushes the request content to the player through `streamer.send`
    val parser = StreamParsers.multiPartByteStreaming(
      bytes => streamer.send(bytes).map(analyzeResult(streamer, bytes, _)),
      maxSize,
      errorHandler
    )
    Action(parser): request =>
      log.info(s"Served file to '$user'.")
      streamer.close()
      val remoteIP = request.remoteAddress
      request.body.files.foreach: file =>
        log.info(
          s"Pushed file '${file.filename}' of size ${file.ref} bytes from '$remoteIP' to user '$user'."
        )
      streamer.isReceivingStream = false
      Ok

  protected def analyzeResult(
    dest: StreamEndpoint,
    bytes: ByteString,
    result: QueueOfferResult
  ): Unit =
    val suffix = s" for ${bytes.length} bytes"
    result match
      case Enqueued    => ()
      case Dropped     => log.warn(s"Offer dropped$suffix")
      case Failure(t)  => log.error(s"Offer failed$suffix", t)
      case QueueClosed => () // log.error(s"Queue closed$suffix")

  private def streamRefusalResponse(user: Username): EssentialAction =
    Action: request =>
      log.warn(
        s"Refused upload from '${request.remoteAddress}' to user '$user' because another upload to that user is currently in progress."
      )
      BadRequest(
        JsonMessages
          .failure("Concurrent streaming to the same player is not supported; try again later.")
      )

  /** Authenticated action for phones.
    *
    * Checks that credentials are set in the header and that a corresponding player is connected
    * with the same userid.
    */
  private def PhoneSecureAction(f: PlayerClient => EssentialAction): EssentialAction =
    authenticatedAsync(authenticatePhone, onUnauthorized)(f)

  /** Checks that credentials in the header exist, but does not check whether a corresponding player
    * is connected. So this validation is not sufficient to allow file uploads.
    */
  def PhoneLimitedSecureAction(f: Username => EssentialAction) =
    authenticatedAsync(authHeader, onUnauthorized)(f)

  /** Authenticated action for connected players.
    *
    * Checks the session username and requires a previously open player connection.
    */
  def PlayerSecureAction(f: PlayerClient => EssentialAction) =
    authenticatedAsync(beams.authPlayer, onUnauthorized)(f)

  /** Checks that a username is set in the session.
    */
  def PlayerLimitedSecureAction(f: Username => EssentialAction) =
    authenticatedAsync(beams.authUser, onUnauthorized)(a => f(a))

  private def authenticatePhone(request: RequestHeader): Future[Outcome[PlayerClient]] =
    headerUsername(request)
      .map: user =>
        beams.findPlayer(user)
      .getOrElse:
        Future.successful(None)
      .map(_.toRight(InvalidCredentials(request)))

  private def authHeader(rh: RequestHeader) =
    Future.successful(headerUsername(rh).toRight(InvalidCredentials(rh)))

  private def headerUsername(request: RequestHeader): Option[Username] =
    Auth
      .basicCredentials(request)
      .flatMap: creds =>
        val isValid = Home.validateCredentials(creds)
        if isValid then Option(creds.username) else None

//  override def validateCredentials(creds: BasicCredentials): Future[Boolean] =
//    Future.successful(Home.validateCredentials(creds))

  /** Async version of Security.Authenticated.
    *
    * @param auth
    *   auth function
    * @param onUnauthorized
    *   callback if auth fails
    * @param action
    *   authenticated action
    * @return
    *   an authenticated action
    */
  def authenticatedAsync[A](
    auth: RequestHeader => Future[Either[AuthFailure, A]],
    onUnauthorized: AuthFailure => Result
  )(action: A => EssentialAction): EssentialAction =
    EssentialAction: rh =>
      val futureAccumulator = auth(rh) map { authResult =>
        authResult.fold(
          failure => Accumulator.done(onUnauthorized(failure)),
          success => action(success).apply(rh)
        )
      }
      Accumulator.flatten(futureAccumulator)(mat)

  def onUnauthorized(failure: AuthFailure) = Unauthorized
