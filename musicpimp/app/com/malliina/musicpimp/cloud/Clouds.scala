package com.malliina.musicpimp.cloud

import cats.effect.IO

import java.nio.file.{Files, Path}
import java.util.concurrent.atomic.AtomicReference
import org.apache.pekko.NotUsed
import org.apache.pekko.actor.{Cancellable, Scheduler}
import org.apache.pekko.stream.scaladsl.{Keep, Sink, Source}
import org.apache.pekko.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import com.malliina.concurrent.Execution.cached
import com.malliina.concurrent.FutureOps
import com.malliina.file.FileUtilities
import com.malliina.http.FullUrl
import com.malliina.musicpimp.audio.MusicPlayer
import com.malliina.musicpimp.cloud.Clouds.log
import com.malliina.musicpimp.db.FullText
import com.malliina.musicpimp.json.CommonMessages
import com.malliina.musicpimp.models.*
import com.malliina.musicpimp.scheduler.json.JsonHandler
import com.malliina.musicpimp.util.FileUtil
import com.malliina.streams.StreamsUtil
import io.circe.Json
import play.api.Logger

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object Clouds:
  private val log = Logger(getClass)
  val idFile = FileUtil.localPath("cloud.txt")

  def isEnabled = Files.exists(idFile)

  def loadID(): Option[CloudID] = readFirstLine(idFile).map(CloudID.apply)

  def readFirstLine(file: Path): Option[String] =
    Try(FileUtilities.firstLine(file)).toOption

  def saveID(id: CloudID): Unit = writeOneLine(idFile, id.id)

  def writeOneLine(file: Path, text: String): Unit =
    if !Files.exists(file) then Try(Files.createFile(file))
    FileUtilities.writerTo(file)(_.println(text))

class Clouds(
  player: MusicPlayer,
  alarmHandler: JsonHandler,
  deps: Deps,
  fullText: FullText[IO],
  cloudEndpoint: FullUrl,
  scheduler: Scheduler
)(implicit mat: Materializer)
  extends AutoCloseable:
  private val clientRef: AtomicReference[CloudSocket] = new AtomicReference(newSocket(None))
  private val timer = Source.tick(3.seconds, 60.seconds, 0)
  private var poller: Option[Cancellable] = None
  private val MaxFailures = 720
  private var successiveFailures = 0
  private val notConnected = Disconnected("Not connected.")
  private val eventHub = StreamsUtil.connectedStream[CloudEvent]()
  private var activeSubscription: Option[UniqueKillSwitch] = None

  private val currentState: AtomicReference[CloudEvent] =
    new AtomicReference[CloudEvent](notConnected)
  val connection: Source[CloudEvent, NotUsed] = eventHub.source

  def updateState(state: CloudEvent): Unit =
    currentState.set(state)
    eventHub.send(state)

  def emitLatest(): Unit = eventHub.send(currentState.get())
  def client = clientRef.get()
  def cloudHost = client.cloudHost
  def uri = client.uri
  def isConnected = client.isConnected

  def init(): Unit =
    log.info("Initializing cloud connection...")
    ensureConnectedIfEnabled()
    maintainConnectivity()

  def maintainConnectivity(): Unit =
    if poller.isEmpty then
      val cancellable = timer.toMat(Sink.foreach(_ => ensureConnectedIfEnabled()))(Keep.left).run()
      log.info(s"Maintaining connectivity to the cloud: '${Clouds.isEnabled}'.")
      poller = Option(cancellable)

  def ensureConnectedIfEnabled(): Unit = Try:
    if Clouds.isEnabled && !client.isConnected then
      log.info(s"Attempting to reconnect to the cloud at '${client.uri}'...")
      connect(Clouds.loadID()).recoverAll: t =>
        log.warn(s"Unable to connect to the cloud at '${client.uri}'.", t)
        successiveFailures += 1
        if successiveFailures == MaxFailures then
          log.info(
            s"Connection attempts to the cloud at '${client.uri}' have failed $MaxFailures times in a row, giving up"
          )
          successiveFailures = 0
          disconnect("Disconnected after sustained failures.")
        "Recovered" // -Xlint won't accept returning Any
    else client.sendMessage(CommonMessages.ping)

  def connect(id: Option[CloudID]): Future[CloudID] = reg:
    activeSubscription.foreach(_.shutdown())
    updateState(Connecting)
    val prep = async:
      val name = id.map(i => s"'$i'") getOrElse "a random client"
      log.debug(s"Connecting as $name to ${client.uri}...")
      val old = clientRef.getAndSet(newSocket(id))
      closeAnyConnection(old)

      val connectionSink = Sink.foreach[CloudID]: id =>
        updateState(Connected(id))
      val (_, killSwitch) = client.registrations
        .watchTermination()(Keep.right)
        .viaMat(KillSwitches.single)(Keep.both)
        .mapMaterializedValue:
          case (done, ks) =>
            val f = done.transform: t =>
              updateState(Disconnected("The connection failed."))
              t
            (f, ks)
        .to(connectionSink)
        .run()
      activeSubscription = Option(killSwitch)
    for
      _ <- prep
      id <- client.connectID()
      savedId <- onConnected(id)
    yield savedId

  def onConnected(id: CloudID): Future[CloudID] = async:
    successiveFailures = 0
    Clouds.saveID(id)
    log.info(s"Connected to ${client.uri}")
    maintainConnectivity()
    id

  def newSocket(id: Option[CloudID]): CloudSocket =
    CloudSocket.build(
      player,
      id orElse Clouds.loadID(),
      cloudEndpoint,
      alarmHandler,
      scheduler,
      fullText,
      deps,
      mat
    )

  def disconnectAndForgetAsync(): Future[Boolean] =
    async(disconnectAndForget("Disconnected by user."))

  def async[T](code: => T) = Future(code)(cached)

  def disconnectAndForget(reason: String) =
    disconnect(reason)
    Files.deleteIfExists(Clouds.idFile)

  def disconnect(reason: String): Unit =
    updateState(Disconnecting)
    stopPolling()
    closeAnyConnection(client)
    updateState(Disconnected(reason))
    activeSubscription.foreach(_.shutdown())

  def closeAnyConnection(closeable: CloudSocket): Unit =
    val wasConnected = closeable.isConnected
    closeable.close()
    if wasConnected then log debug s"Disconnected from the cloud at ${closeable.uri}"

  def stopPolling(): Unit =
    poller.foreach(_.cancel())
    poller = None

  def registration: Future[CloudID] = reg(Future.failed(CloudSocket.notConnected))

  def sendIfConnected(msg: Json): SendResult =
    if client.isConnected then
      client.send(msg) match
        case Success(()) => MessageSent
        case Failure(t)  => SendFailure(t)
    else NotConnected

  private def reg(ifDisconnected: => Future[CloudID]) =
    if client.isConnected then client.registration
    else ifDisconnected

  def close(): Unit = eventHub.shutdown()

trait SendResult

case object MessageSent extends SendResult
case object NotConnected extends SendResult
case class SendFailure(t: Throwable) extends SendResult
