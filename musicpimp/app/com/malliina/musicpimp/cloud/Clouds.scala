package com.malliina.musicpimp.cloud

import java.nio.file.{Files, Path}
import java.util.concurrent.atomic.AtomicReference

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.file.FileUtilities
import com.malliina.http.FullUrl
import com.malliina.musicpimp.cloud.Clouds.log
import com.malliina.musicpimp.models.{Connected, Connecting, Disconnected, Disconnecting}
import com.malliina.musicpimp.models.{CloudEvent, CloudID}
import com.malliina.musicpimp.scheduler.json.JsonHandler
import com.malliina.musicpimp.util.FileUtil
import com.malliina.util.Utils
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

  def saveID(id: CloudID): Unit = writeOneLine(idFile, id.id)

  def writeOneLine(file: Path, text: String): Unit = {
    if (!Files.exists(file)) {
      Try(Files.createFile(file))
    }
    FileUtilities.writerTo(file)(_.println(text))
  }
}

class Clouds(alarmHandler: JsonHandler, deps: Deps, cloudEndpoint: FullUrl) {
  private val clientRef: AtomicReference[CloudSocket] = new AtomicReference(newSocket(None))
  private val timer = Observable.interval(60.seconds)
  private var poller: Option[Subscription] = None
  val MaxFailures = 720
  var successiveFailures = 0
  val notConnected = Disconnected("Not connected.")
  private val registrations = BehaviorSubject[CloudEvent](notConnected).toSerialized
  private var activeSubscription: Option[Subscription] = None

  val connection: Observable[CloudEvent] = registrations

  def client = clientRef.get()

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
      connect(Clouds.loadID()).recoverAll { t =>
        log.warn(s"Unable to connect to the cloud at ${client.uri}", t)
        successiveFailures += 1
        if (successiveFailures == MaxFailures) {
          log info s"Connection attempts to the cloud have failed $MaxFailures times in a row, giving up"
          successiveFailures = 0
          disconnect("Disconnected after sustained failures.")
        }
        "Recovered" // -Xlint won't accept returning Any
      }
    }
  }

  def connect(id: Option[CloudID]): Future[CloudID] = reg {
    activeSubscription.foreach(_.unsubscribe())
    registrations onNext Connecting
    val prep = async {
      val name = id.map(i => s"'$i'") getOrElse "a random client"
      log debug s"Connecting as $name to ${client.uri}..."
      val old = clientRef.getAndSet(newSocket(id))
      closeAnyConnection(old)
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
    log info s"Connected to ${client.uri}"
    maintainConnectivity()
    id
  }


  def newSocket(id: Option[CloudID]): CloudSocket =
    CloudSocket.build(id orElse Clouds.loadID(), cloudEndpoint, alarmHandler, deps)

  def disconnectAndForgetAsync(): Future[Boolean] =
    async(disconnectAndForget("Disconnected by user."))

  def async[T](code: => T) = Future(code)(cached)

  def disconnectAndForget(reason: String) = {
    disconnect(reason)
    Files.deleteIfExists(Clouds.idFile)
  }

  def disconnect(reason: String) = {
    registrations onNext Disconnecting
    stopPolling()
    closeAnyConnection(client)
    registrations onNext Disconnected(reason)
    activeSubscription.foreach(_.unsubscribe())
  }

  def closeAnyConnection(closeable: CloudSocket) = {
    val wasConnected = closeable.isConnected
    closeable.close()
    if (wasConnected) {
      log debug s"Disconnected from the cloud at ${closeable.uri}"
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
