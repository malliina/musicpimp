package com.mle.musicpimp.cloud

import com.mle.musicpimp.audio.{MusicPlayer, PlaybackMessageHandler}
import com.mle.musicpimp.cloud.CloudSocket.{hostPort, httpProtocol}
import com.mle.musicpimp.cloud.CloudStrings.{BODY, REGISTERED, REQUEST_ID, UNREGISTER}
import com.mle.musicpimp.cloud.PimpMessages._
import com.mle.musicpimp.db.PimpDb
import com.mle.musicpimp.http.TrustAllMultipartRequest
import com.mle.musicpimp.json.JsonMessages
import com.mle.musicpimp.json.JsonStrings._
import com.mle.musicpimp.library.Library
import com.mle.musicpimp.scheduler.ScheduledPlaybackService
import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import com.mle.play.json.JsonStrings.{CMD, EVENT}
import com.mle.play.json.SimpleCommand
import com.mle.rx.Observables
import com.mle.util.Util
import com.mle.ws.JsonWebSocketClient
import controllers.{Alarms, Rest}
import play.api.libs.json._

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Future, Promise}
import scala.util.Try

/**
 * @author Michael
 */
class CloudSocket(uri: String, username: String, password: String)
  extends JsonWebSocketClient(uri, username, password) {
  private val registrationPromise = Promise[String]()

  val registration = registrationPromise.future

  def connectID(): Future[String] = connect().flatMap(_ => registration)

  def unregister() = Try(send(SimpleCommand(UNREGISTER)))

  /**
   * Reconnections are currently not supported; only call this method once per instance.
   *
   * Impl: On subsequent calls, the returned future will always be completed regardless of connection result
   *
   * @return a future that completes when the connection has successfully been established
   */
  override def connect(): Future[Unit] = {
    log debug s"Connecting as user: $username"
    Observables.timeoutAfter(10.seconds, registrationPromise)
    super.connect()
  }

  override def onMessage(json: JsValue): Unit = {
    log info s"Got message: $json"
    try {
      // attempts to handle the message as a request, then if that fails as an event, if all fails handles the error
      ((parseRequest(json) map (pair => handleRequest(pair._1, pair._2))) orElse
        (parseEvent(json) map handleEvent)) recoverTotal (err => handleError(err, json))
    } catch {
      case e: Exception =>
        (json \ REQUEST_ID).validate[String].map(request => {
          sendJsonResponse(JsonMessages.failure(s"The MusicPimp server was unable to deal with the request: $json"), request)
        })
    }
  }

  def parseRequest(json: JsValue): JsResult[(PimpMessage, String)] = {
    val reqResult = (json \ REQUEST_ID).validate[String]
    val body = json \ BODY
    val cmd = (body \ CMD).validate[String]
    val requestMessage: JsResult[PimpMessage] = reqResult.flatMap(req => cmd.flatMap {
      case VERSION => JsSuccess(GetVersion)
      case TRACK => body.validate[Track]
      case PING => JsSuccess(Ping)
      case AUTHENTICATE => body.validate[Authenticate]
      case ROOT_FOLDER => JsSuccess(RootFolder)
      case FOLDER => body.validate[Folder]
      case SEARCH => body.validate[Search]
      case ALARMS => JsSuccess(GetAlarms)
      case ALARMS_EDIT => JsSuccess(AlarmEdit(body))
      case ALARMS_ADD => JsSuccess(AlarmAdd(body))
      case STATUS => JsSuccess(GetStatus)
      case other => JsError(s"Unknown JSON command: $other in $json")
    })
    reqResult.flatMap(req => requestMessage.map(msg => (msg, req)))
  }

  def parseEvent(json: JsValue): JsResult[PimpMessage] = {
    val event = (json \ EVENT).validate[String]
    val eventMessage = event.flatMap {
      case REGISTERED => json.validate[RegistrationEvent]
      case other => JsError(s"Unknown JSON event: $other in $json")
    }
    eventMessage orElse JsSuccess(PlaybackMessage(json))
  }

  def handleRequest(message: PimpMessage, request: String) = {
    message match {
      case GetStatus => sendResponse(MusicPlayer.status, request)
      case t: Track => upload(t, request)
      case RootFolder => sendResponse(Library.rootFolder, request)
      case Folder(id) =>
        val json = (Library folder id).map(Json.toJson(_)) getOrElse JsonMessages.failure(s"Unable to find folder with ID: $id")
        sendJsonResponse(json, request)
      case Search(term, limit) =>
        val result = PimpDb.fullText(term, limit)
        sendResponse(result, request)
      case PingAuth => sendJsonResponse(JsonMessages.Version, request)
      case Ping => sendJsonResponse(JsonMessages.Ping, request)
      case GetAlarms =>
        implicit val writer = Alarms.alarmWriter
        sendResponse(ScheduledPlaybackService.status, request)
      case AlarmEdit(payload) =>
        Alarms.handle(payload)
        sendAckResponse(request)
      case AlarmAdd(payload) =>
        Alarms.handle(payload)
        sendAckResponse(request)
      case Authenticate(user, pass) =>
        val isValid = Rest.validateCredentials(user, pass)
        val response =
          if (isValid) JsonMessages.Version
          else JsonMessages.failure("Invalid credentials")
        sendJsonResponse(response, request)
      case GetVersion => sendJsonResponse(JsonMessages.Version, request)
      case RegistrationEvent(event, id) => registrationPromise trySuccess id
      case PlaybackMessage(payload) => PlaybackMessageHandler handleMessage payload
    }
  }

  def handleEvent(e: PimpMessage) = {
    e match {
      case RegistrationEvent(event, id) => registrationPromise trySuccess id
      case PlaybackMessage(payload) => PlaybackMessageHandler handleMessage payload
    }
  }

  def requestJson(request: String) = Json.obj(REQUEST_ID -> request)

  def sendResponse[T](response: T, request: String)(implicit writer: Writes[T]): Unit =
    sendJsonResponse(Json.toJson(response), request)

  def sendJsonResponse(response: JsValue, request: String) = {
    val payload = requestJson(request) + (BODY -> response)
    sendLogged(payload, request)
  }

  def sendAckResponse(request: String) = sendLogged(requestJson(request), request)

  def sendLogged(payload: JsValue, request: String) = {
    send(payload)
    log debug s"Responded to request: $request with payload: $payload"
  }

  def handleError(errors: JsError, json: JsValue): Unit = log warn errorMessage(errors, json)

  def errorMessage(errors: JsError, json: JsValue): String = {
    s"JSON error: $errors. Message: $json"
  }

  override def onClose(): Unit = failRegistration(CloudSocket.connectionClosed)

  override def onError(e: Exception): Unit = failRegistration(e)

  def failRegistration(e: Exception) = registrationPromise tryFailure e

  def upload(track: Track, request: String) = Future {
    val trackID = track.id
    val uploadUri = s"$httpProtocol://$hostPort/track"
    Util.using(new TrustAllMultipartRequest(uploadUri))(req => {
      req.addHeaders(REQUEST_ID -> request)
      // TODO if the track cannot be found, this should be communicated to the client more elegantly
      Library.findAbsolute(trackID).fold(log warn s"Unable to find track: $trackID")(path => {
        req addFile path
        log info s"Uploading file: $path to: $uploadUri as response to request: $request"
      })
      req.execute()
      log info s"Upload complete of: $request"
    })
  }
}

object CloudSocket {
  val hostPort = "cloud.musicpimp.org"
  val httpProtocol = "https"
  val socketProtocol = "wss"
  //  val hostPort = "localhost:9000"

  def build(id: Option[String]) = new CloudSocket(s"$socketProtocol://$hostPort/servers/ws2", id getOrElse "", "pimp")

  val notConnected = new Exception("Not connected.")
  val connectionClosed = new Exception("Connection closed.")
}
