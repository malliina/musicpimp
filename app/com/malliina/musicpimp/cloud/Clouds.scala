package com.malliina.musicpimp.cloud

import java.nio.file.{Files, Path}

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.file.FileUtilities
import com.malliina.musicpimp.cloud.Clouds.log
import com.malliina.musicpimp.util.FileUtil
import com.malliina.play.json.SimpleCommand
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

  def saveID(id: CloudID) = writeOneLine(idFile, id.id)

  def writeOneLine(file: Path, text: String) = {
    if (!Files.exists(file)) {
      Try(Files.createFile(file))
    }
    FileUtilities.writerTo(file)(_.println(text))
  }
}

class Clouds(deps: Deps) {
  private var client: CloudSocket = newSocket(None)
  val timer = Observable.interval(60.seconds)
  var poller: Option[Subscription] = None
  val MaxFailures = 720
  var successiveFailures = 0
  private val registrations = BehaviorSubject[Option[CloudID]](None).toSerialized
  private var activeSubscription: Subscription = client.registrations.subscribe(registrations)

  def connection: Observable[Option[CloudID]] = registrations

  def cloudHost = client.cloudHost

  def uri = client.uri

  def isConnected = client.isConnected

  def init(): Unit = {
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
    closeAnyConnection()
    val name = id.map(_.id) getOrElse "a random client"
    log info s"Connecting to ${client.uri} as $name..."
    activeSubscription.unsubscribe()
    client = newSocket(id)
    activeSubscription = client.registrations.subscribe(registrations)
    client.connectID() map { id =>
      successiveFailures = 0
      Clouds.saveID(id)
      log info s"Connected to ${client.uri}"
      maintainConnectivity()
      id
    }
  }

  def newSocket(id: Option[CloudID]) = CloudSocket.build(id orElse Clouds.loadID(), deps)

  def disconnectAndForget() = {
    client sendMessage SimpleCommand(CloudStrings.Unregister)
    disconnect()
    Files.deleteIfExists(Clouds.idFile)
  }

  def disconnect() = {
    stopPolling()
    closeAnyConnection()
  }

  def closeAnyConnection() = {
    val wasConnected = client.isConnected
    client.close()
    if (wasConnected) {
      log info s"Disconnected from the cloud at ${client.uri}"
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
