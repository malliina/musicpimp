package com.malliina.musicpimp.cloud

import java.nio.file.{Files, Path}

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.file.FileUtilities
import com.malliina.musicpimp.cloud.Clouds.log
import com.malliina.musicpimp.models.CloudID
import com.malliina.musicpimp.util.FileUtil
import com.malliina.play.http.FullUrl
import com.malliina.util.Utils
import controllers.musicpimp.CloudEvent
import controllers.musicpimp.CloudEvent.{Connected, Connecting, Disconnected, Disconnecting}
import play.api.Logger
import play.api.libs.json.JsValue
import rx.lang.scala.subjects.BehaviorSubject
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object Clouds {
  private val log = Logger(getClass)
  val idFile = FileUtil.localPath("cloud.txt")

  def isEnabled = Files exists idFile

  def loadID(): Option[CloudID] = readFirstLine(idFile).map(CloudID.apply)

  def readFirstLine(file: Path): Option[String] =
    Utils.opt[String, Exception](FileUtilities.firstLine(file))

  def saveID(id: CloudID) = writeOneLine(idFile, id.id)

  def writeOneLine(file: Path, text: String) = {
    if (!Files.exists(file)) {
      Try(Files.createFile(file))
    }
    FileUtilities.writerTo(file)(_.println(text))
  }
}

class Clouds(deps: Deps, cloudEndpoint: FullUrl) {
  private var client: CloudSocket = newSocket(None)
  private val timer = Observable.interval(60.seconds)
  private var poller: Option[Subscription] = None
  val MaxFailures = 720
  var successiveFailures = 0
  val notConnected = Disconnected("Not connected.")
  val registrations = BehaviorSubject[CloudEvent](notConnected).toSerialized
  private var activeSubscription: Option[Subscription] = None

  val connection: Observable[CloudEvent] = registrations

  def cloudHost = client.cloudHost

  def uri = client.uri

  def isConnected = client.isConnected

  def init(): Unit = {
    log info s"Initializing cloud connection..."
    ensureConnectedIfEnabled()
    maintainConnectivity()
  }

  def maintainConnectivity(): Unit = {
    if (poller.isEmpty) {
      val subscription = timer.subscribe(
        _ => ensureConnectedIfEnabled(),
        (err: Throwable) => log.error("Cloud poller failed", err),
        () => log.error("Cloud poller completed")
      )
      poller = Some(subscription)
    }
  }

  def ensureConnectedIfEnabled(): Unit = Try {
    if (Clouds.isEnabled && !client.isConnected) {
      log info s"Attempting to reconnect to the cloud..."
      connect(Clouds.loadID()).recoverAll(t => {
        log.warn(s"Unable to connect to the cloud at ${client.uri}", t)
        successiveFailures += 1
        if (successiveFailures == MaxFailures) {
          log info s"Connection attempts to the cloud have failed $MaxFailures times in a row, giving up"
          successiveFailures = 0
          disconnect()
        }
        "Recovered" // -Xlint won't accept returning Any
      })
    }
  }

  def connect(id: Option[CloudID]): Future[CloudID] = reg {
    activeSubscription.foreach(_.unsubscribe())
    registrations onNext Connecting
    val prep = async {
      closeAnyConnection()
      val name = id.map(i => s"'$i'") getOrElse "a random client"
      log debug s"Connecting as $name to ${client.uri}..."
      client = newSocket(id)
      val sub = client.registrations.subscribe(
        id => registrations.onNext(Connected(id)),
        _ => registrations.onNext(Disconnected("The connection failed.")),
        () => ()
      )
      activeSubscription = Option(sub)
    }
    for {
      _ <- prep
      id <- client.connectID()
      savedId <- onConnected(id)
    } yield savedId
  }

  def onConnected(id: CloudID): Future[CloudID] = async {
    successiveFailures = 0
    Clouds.saveID(id)
    log debug s"Connected to ${client.uri}"
    maintainConnectivity()
    id
  }

  def async[T](code: => T) = Future(code)(cached)

  def newSocket(id: Option[CloudID]) =
    CloudSocket.build(id orElse Clouds.loadID(), cloudEndpoint, deps)

  def disconnectAndForgetAsync(): Future[Boolean] = {
    Future(disconnectAndForget())(cached)
  }

  def disconnectAndForget() = {
    disconnect()
    Files.deleteIfExists(Clouds.idFile)
  }

  def disconnect() = {
    registrations onNext Disconnecting
    stopPolling()
    closeAnyConnection()
  }

  def closeAnyConnection() = {
    val wasConnected = client.isConnected
    client.close()
    if (wasConnected) {
      log debug s"Disconnected from the cloud at ${client.uri}"
    }
  }

  def stopPolling(): Unit = {
    poller.foreach(_.unsubscribe())
    poller = None
  }

  def registration: Future[CloudID] = reg(Future.failed(CloudSocket.notConnected))

  def sendIfConnected(msg: JsValue): SendResult = {
    if (client.isConnected) {
      client send msg match {
        case Success(()) => MessageSent
        case Failure(t) => SendFailure(t)
      }
    } else {
      NotConnected
    }
  }

  private def reg(ifDisconnected: => Future[CloudID]) = {
    if (client.isConnected) client.registration
    else ifDisconnected
  }

  trait SendResult

  case object MessageSent extends SendResult

  case object NotConnected extends SendResult

  case class SendFailure(t: Throwable) extends SendResult

}
