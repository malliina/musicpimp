package com.mle.musicpimp.cloud

import java.nio.file.{Files, Path}

import com.mle.concurrent.FutureImplicits.RichFuture
import com.mle.file.FileUtilities
import com.mle.util.{Log, Utils}
import play.api.libs.json.JsValue
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.{Failure, Success, Try}

/**
 * @author Michael
 */
object Clouds extends Log {
  val idFile = FileUtilities pathTo "cloud.txt"
  var client: CloudSocket = newSocket(None)
  val timer = Observable.interval(30.minutes)
  var poller: Option[Subscription] = None
  val MAX_FAILURES = 50
  var successiveFailures = 0

  def init() = {
    connectIfEnabled()
    maintainConnectivity()
  }

  def connectIfEnabled() = {
    if (isEnabled && !client.isConnected) {
      connect().recoverAll(t => {
        log.warn(s"Unable to connect to the cloud.", t)
        successiveFailures += 1
        if (successiveFailures == MAX_FAILURES) {
          log info s"Connection attempts to the cloud have failed $MAX_FAILURES times in a row, giving up."
          successiveFailures = 0
          disconnect()
        }
      })
    }
  }

  def maintainConnectivity() = {
    poller = Some(timer.subscribe(_ => connectIfEnabled()))
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

  def connect(id: Option[String] = None): Future[String] = reg {
    disconnect()
    client = newSocket(id)
    maintainConnectivity()
    client.connect().flatMap(_ => client.registration).map(id => {
      successiveFailures = 0
      saveID(id)
      id
    })
  }

  def disconnectAndForget() = {
    disconnect()
    Files.deleteIfExists(idFile)
  }

  def disconnect() = {
    poller.foreach(_.unsubscribe())
    poller = None
    client.close()
  }

  def registration: Future[String] = reg(Future.failed(CloudSocket.notConnected))

  def sendIfConnected(msg: JsValue): SendResult = {
    if (client.isConnected) {
      Try(client send msg) match {
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
