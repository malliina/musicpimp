package com.mle.musicpimp.cloud

import java.nio.file.{Files, Path}

import com.mle.concurrent.FutureOps
import com.mle.file.{FileUtilities, StorageFile}
import com.mle.musicpimp.util.FileUtil
import com.mle.concurrent.ExecutionContexts.cached
import com.mle.play.json.SimpleCommand
import com.mle.util.{Log, Utils}
import play.api.libs.json.JsValue
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

/**
 * @author Michael
 */
object Clouds extends Log {
  val idFile = FileUtil.localPath("cloud.txt")
  var client: CloudSocket = newSocket(None)
  val timer = Observable.interval(30.minutes)
  var poller: Option[Subscription] = None
  val MAX_FAILURES = 50
  var successiveFailures = 0

  def init(): Unit = {
    ensureConnectedIfEnabled()
    maintainConnectivity()
  }

  def ensureConnectedIfEnabled(): Unit = {
    if (isEnabled && !client.isConnected) {
      connect(loadID()).recoverAll(t => {
        log.warn(s"Unable to connect to the cloud at ${client.uri}", t)
        successiveFailures += 1
        if (successiveFailures == MAX_FAILURES) {
          log info s"Connection attempts to the cloud have failed $MAX_FAILURES times in a row, giving up"
          successiveFailures = 0
          disconnect()
        }
        "Recovered" // -Xlint won't accept returning Any
      })
    }
  }

  def maintainConnectivity(): Unit = {
    stopPolling()
    poller = Some(timer.subscribe(_ => ensureConnectedIfEnabled()))
  }

  def isEnabled = Files exists idFile

  def loadID(): Option[String] = readFirstLine(idFile)

  def readFirstLine(file: Path): Option[String] = Utils.opt[String, Exception](FileUtilities.firstLine(file))

  def saveID(id: String) = writeOneLine(idFile, id)

  def writeOneLine(file: Path, text: String) = {
    if (!Files.exists(file)) {
      Try(Files.createFile(file))
    }
    FileUtilities.writerTo(file)(_.println(text))
  }

  def newSocket(id: Option[String]) = CloudSocket build (id orElse loadID())

  def connect(id: Option[String]): Future[String] = reg {
    disconnect()
    log info s"Connecting to ${client.uri} as $id..."
    client = newSocket(id)
    client.connectID().map(id => {
      successiveFailures = 0
      saveID(id)
      log info s"Connected to ${client.uri}"
      maintainConnectivity()
      id
    })
  }

  def disconnectAndForget() = {
    client sendMessage SimpleCommand(CloudStrings.UNREGISTER)
    disconnect()
    Files.deleteIfExists(idFile)
  }

  def disconnect() = {
    stopPolling()
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

  def registration: Future[String] = reg(Future.failed(CloudSocket.notConnected))

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

  private def reg(ifDisconnected: => Future[String]) = {
    if (client.isConnected) client.registration
    else ifDisconnected
  }

  trait SendResult

  case object MessageSent extends SendResult

  case object NotConnected extends SendResult

  case class SendFailure(t: Throwable) extends SendResult

}
