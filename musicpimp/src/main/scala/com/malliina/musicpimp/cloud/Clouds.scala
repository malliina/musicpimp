package com.malliina.musicpimp.cloud

import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxFlatMapOps, toFlatMapOps, toFunctorOps}

import java.nio.file.{Files, Path}
import java.util.concurrent.atomic.AtomicReference
import com.malliina.file.FileUtilities
import com.malliina.http.FullUrl
import com.malliina.musicpimp.audio.{MusicPlayer, ServerMessage, TrackJson, TrackMeta}
import com.malliina.musicpimp.cloud.Clouds.log
import com.malliina.musicpimp.cloud.SendResult.{MessageSent, NotConnected, SendFailure}
import com.malliina.musicpimp.db.FullText
import com.malliina.musicpimp.json.CommonMessages
import com.malliina.musicpimp.models.*
import com.malliina.musicpimp.scheduler.json.JsonHandler
import com.malliina.musicpimp.util.FileUtil
import com.malliina.util.AppLogger
import io.circe.{Codec, Encoder, Json}
import fs2.Stream
import fs2.concurrent.Topic

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

object Clouds:
  private val log = AppLogger(getClass)

  val idFile = FileUtil.localPath("cloud.txt")

  def isEnabled = Files.exists(idFile)

  def loadID(): Option[CloudID] = readFirstLine(idFile).map(CloudID.apply)

  def readFirstLine(file: Path): Option[String] =
    Try(FileUtilities.firstLine(file)).toOption

  def saveID(id: CloudID): Unit = writeOneLine(idFile, id.id)

  def writeOneLine(file: Path, text: String): Unit =
    if !Files.exists(file) then Try(Files.createFile(file))
    FileUtilities.writerTo(file)(_.println(text))

  def prod[F[_]: Async](
    player: MusicPlayer[F],
    alarmHandler: JsonHandler[F],
    deps: Deps[F],
    fullText: FullText[F],
    cloudEndpoint: FullUrl
  ): F[Clouds[F]] =
    for
      eventHub <- Topic[F, CloudEvent]
      halts <- Topic[F, Boolean]
      initial <- CloudSocket.build(
        player,
        Clouds.loadID(),
        cloudEndpoint,
        alarmHandler,
        fullText,
        deps
      )
    yield Clouds(initial, eventHub, halts, player, alarmHandler, deps, fullText, cloudEndpoint)

  def playerEventsToPimpcloud[F[_]: Async](
    player: MusicPlayer[F],
    clouds: Clouds[F]
  ): Stream[F, SendResult] =
    given tm: Codec[TrackMeta] = TrackJson.format(clouds.cloudHost)
    val cloudWriter: Encoder[ServerMessage] =
      ServerMessage.jsonWriter(using Encoder[TrackMeta])
    player.allEvents.evalMap(msg => Async[F].delay(clouds.sendIfConnected(cloudWriter(msg))))

class Clouds[F[_]: Async](
  initial: CloudSocket[F],
  eventHub: Topic[F, CloudEvent],
  halts: Topic[F, Boolean],
  player: MusicPlayer[F],
  alarmHandler: JsonHandler[F],
  deps: Deps[F],
  fullText: FullText[F],
  cloudEndpoint: FullUrl
) extends AutoCloseable:
  val F = Async[F]
  private val clientRef: AtomicReference[CloudSocket[F]] = new AtomicReference(initial)

  private val haltStream = halts.subscribe(10)
  private val timer = Stream.awakeEvery(60.seconds).delayBy(3.seconds)
  val events: Stream[F, Unit] =
    timer.evalMap(_ => ensureConnectedIfEnabled()).interruptWhen(haltStream)
  private var isPolling = false
//  private var poller: Option[Cancellable] = None
  private val MaxFailures = 720
  private var successiveFailures = 0
  private val notConnected = Disconnected("Not connected.")
//  private var activeSubscription: Option[UniqueKillSwitch] = None

  private val currentState: AtomicReference[CloudEvent] =
    new AtomicReference[CloudEvent](notConnected)
  val connection: Stream[F, CloudEvent] = eventHub.subscribe(100)

  private def updateState(state: CloudEvent): F[Unit] =
    currentState.set(state)
    eventHub.publish1(state).void

  def emitLatest(): F[Unit] = eventHub.publish1(currentState.get()).void
  def client: CloudSocket[F] = clientRef.get()
  def cloudHost = client.cloudHost
  def uri = client.uri
  def isConnected = client.isConnected

//  def init(): Unit =
//    log.info("Initializing cloud connection...")
//    ensureConnectedIfEnabled()
//    maintainConnectivity()

//  private def maintainConnectivity(): Unit =
//    if !isPolling then
//      timer.evalMap(_ => ensureConnectedIfEnabled()).interruptWhen(haltStream)
//      log.info(s"Maintaining connectivity to the cloud: '${Clouds.isEnabled}'.")
//      isPolling = true

  private def ensureConnectedIfEnabled(): F[Unit] =
    if Clouds.isEnabled && !client.isConnected then
      log.info(s"Attempting to reconnect to the cloud at '${client.uri}'...")
      connect(Clouds.loadID()).void.handleErrorWith: t =>
        log.warn(s"Unable to connect to the cloud at '${client.uri}'.", t)
        successiveFailures += 1
        if successiveFailures == MaxFailures then
          log.info(
            s"Connection attempts to the cloud at '${client.uri}' have failed $MaxFailures times in a row, giving up"
          )
          successiveFailures = 0
          disconnect("Disconnected after sustained failures.").void
        else F.unit // -Xlint won't accept returning Any
    else F.fromTry(client.sendMessage(CommonMessages.ping))

  def connect(id: Option[CloudID]): F[CloudID] = reg:
    updateState(Connecting).flatMap: _ =>
      val name = id.map(i => s"'$i'").getOrElse("a random client")
      log.debug(s"Connecting as $name to ${client.uri}...")
      val regs = newSocket(id).flatMap: socket =>
        val old = clientRef.getAndSet(socket)
        closeAnyConnection(old)
        val connectStream = client.registrations
          .evalMap: id =>
            updateState(Connected(id))
          .handleErrorWith: t =>
            val task = updateState(Disconnected("The connection failed."))
            Stream.eval(task)
          .interruptWhen(haltStream)
        connectStream
          .take(1)
          .compile
          .toList // This is most likely wrong; just wrote it to make it compile for now
      for
        _ <- regs
        id <- client.connectID()
        savedId <- onConnected(id)
      yield savedId

  private def onConnected(id: CloudID): F[CloudID] = F.delay:
    successiveFailures = 0
    Clouds.saveID(id)
    log.info(s"Connected to ${client.uri}")
//    maintainConnectivity()
    id

  def newSocket(id: Option[CloudID]): F[CloudSocket[F]] =
    CloudSocket.build(
      player,
      id.orElse(Clouds.loadID()),
      cloudEndpoint,
      alarmHandler,
      fullText,
      deps
    )

  def disconnectAndForgetAsync(): F[Boolean] =
    disconnectAndForget("Disconnected by user.")

  def disconnectAndForget(reason: String) =
    disconnect(reason) >> F.delay(Files.deleteIfExists(Clouds.idFile))

  def disconnect(reason: String): F[Unit] =
    for
      _ <- updateState(Disconnecting)
      _ = stopPolling()
      _ = closeAnyConnection(client)
      _ <- updateState(Disconnected(reason))
      _ <- halts.publish1(true)
    yield ()

  private def closeAnyConnection(closeable: CloudSocket[F]): Unit =
    val wasConnected = closeable.isConnected
    closeable.close()
    if wasConnected then log.debug(s"Disconnected from the cloud at ${closeable.uri}")

  private def stopPolling(): F[Unit] =
    halts.publish1(true).void

  def registration: F[CloudID] = reg(F.raiseError(CloudSocket.notConnected))

  def sendIfConnected(msg: Json): SendResult =
    if client.isConnected then
      client.send(msg) match
        case Success(()) => MessageSent
        case Failure(t)  => SendFailure(t)
    else NotConnected

  private def reg(ifDisconnected: => F[CloudID]) =
    if client.isConnected then client.registration
    else ifDisconnected

  def close(): Unit = ()

enum SendResult:
  case MessageSent
  case NotConnected
  case SendFailure(t: Throwable)
