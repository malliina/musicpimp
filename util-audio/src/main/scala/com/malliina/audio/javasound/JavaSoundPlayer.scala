package com.malliina.audio.javasound

import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.*
import org.apache.pekko.stream.scaladsl.{Keep, Sink, Source}
import com.malliina.audio.PlaybackEvents.TimeUpdated
import com.malliina.audio.*
import com.malliina.audio.javasound.JavaSoundPlayer.{DefaultRwBufferSize, log}
import com.malliina.audio.meta.OneShotStream
import com.malliina.storage.{StorageInt, StorageLong, StorageSize}
import com.malliina.streams.{EventSink, StreamsUtil}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

object JavaSoundPlayer:
  private val log = LoggerFactory.getLogger(getClass)

  val DefaultRwBufferSize: StorageSize = 4096.bytes

/** A music player. Plays one media source. To change source, for example to change track, create a
  * new player.
  *
  * The user needs to provide the media length and size to enable seek functionality. Seeking
  * streams which cannot be reopened is only supported if InputStream.markSupported() of
  * `media.stream` is true, and even then the support is buggy. markSupported() is true at least for
  * [[java.io.BufferedInputStream]]s.
  *
  * The stream provided in `media` is not by default closed when the player is closed, but if you
  * wish to do so, subclass this player and override `close()` accordingly or mix in trait
  * [[SourceClosing]].
  *
  * I think it's preferred to use an ExecutionContext with one thread only.
  *
  * @see
  *   [[FileJavaSoundPlayer]]
  * @see
  *   [[UriJavaSoundPlayer]]
  * @param media
  *   media info to play
  */
class JavaSoundPlayer(
  val media: OneShotStream,
  readWriteBufferSize: StorageSize = DefaultRwBufferSize
)(implicit mat: Materializer, val ec: ExecutionContext = ExecutionContexts.singleThreadContext)
  extends IPlayer
  with JavaSoundPlayerBase
  with StateAwarePlayer
  with AutoCloseable:

  def this(
    stream: InputStream,
    duration: FiniteDuration,
    size: StorageSize,
    readWriteBufferSize: StorageSize
  )(implicit mat: Materializer) =
    this(OneShotStream(stream, duration, size), readWriteBufferSize)

  val bufferSize = readWriteBufferSize.toBytes.toInt
  protected var stream: InputStream = media.stream
  tryMarkStream()
  private val stateHub = StreamsUtil.connectedStream[PlayerStates.PlayerState]()

  /** I use a Subject because the audio line might change and it seems easier then to keep one
    * subject instead of reacting to each audio line change in each observable (in addition to its
    * events).
    */
  private val pollingSource = Source.tick(500.millis, 500.millis, 0)
  private var lineData: LineData = newLine(stream, stateHub.sink)
  private val active = new AtomicBoolean(false)
  private var playThread: Option[Future[Unit]] = None

  private val timeUpdateHub = StreamsUtil.connectedStream[PlaybackEvents.TimeUpdated]()
  private var latestPos: Duration = position
  val poller = pollingSource
    .to(Sink.foreach: _ =>
      if latestPos != position then
        latestPos = position
        timeUpdateHub.send(TimeUpdated(position)))
    .run()

  /** A stream of time update events. Emits the current playback position, then emits at least one
    * event per second provided that the playback position changes. If there is no progress, for
    * example if playback is stopped, no events are emitted.
    *
    * @return
    *   time update events
    */
  def timeUpdates: Source[TimeUpdated, NotUsed] =
    Source.single(TimeUpdated(position)).concat(timeUpdateHub.source)

  def timeUpdatesKillable: Source[TimeUpdated, UniqueKillSwitch] =
    timeUpdates.viaMat(KillSwitches.single)(Keep.right)

  def isActive = active.get()

  /** @return
    *   the current player state and any future states
    */
  def events: Source[PlayerStates.PlayerState, NotUsed] = stateHub.source

  def eventsKillable: Source[PlayerStates.PlayerState, UniqueKillSwitch] =
    events.viaMat(KillSwitches.single)(Keep.right)

  def audioLine = lineData.line

  def controlDescriptions = audioLine.getControls.map(_.toString)

  def newLine(source: InputStream, sink: EventSink[PlayerStates.PlayerState]): LineData =
    LineData.fromStream(source, sink)

  def supportsSeek = stream.markSupported()

  def play(): Unit =
    lineData.state match
      case PlayerStates.Started =>
        log.info("Start playback issued but playback already started: doing nothing")
      case PlayerStates.Closed =>
        log.warn("Cannot start playback of a closed track.")
      // After end of media, the InputStream is closed and cannot be reused. Therefore this player cannot be used.
      // It's incorrect to call methods on a closed player. In principle we should throw an exception here, but I try
      // to resist the path of the IllegalStateException.
      case _ =>
        startPlayback()

  def stop(): Unit =
    active.set(false)
    audioLine.stop()

  /** Regardless of whether the user seeks backwards or forwards, here is what we do:
    *
    * Reset the stream to its initial position. Skip bytes from the beginning. (Optionally continue
    * playback.)
    *
    * The stream needs to support mark so that we can mark the initial position (constructor).
    * Subsequent calls to reset will therefore go to the initial position. Then we can skip the
    * sufficient amount of bytes and arrive at the correct position. Otherwise seeking would just
    * skip bytes forward every time, relative to the current position.
    *
    * This can still be spectacularly inaccurate if a VBR file is seeked but that is a secondary
    * problem.
    *
    * @param pos
    *   position to seek to
    */
  def seek(pos: Duration): Unit =
    seekProblem
      .map(problem => log.warn(problem))
      .getOrElse:
        val bytes = timeToBytes(pos)
        val skippedBytes = seekBytes(bytes)
        startedFromMicros = bytesToTime(skippedBytes).toMicros

  def seekProblem: Option[String] =
    if lineData.state == PlayerStates.Closed then Some(s"Cannot seek a stream of a closed track.")
    else if !stream.markSupported() then
      Some(
        "Cannot seek because the media stream does not support marking; see InputStream.markSupported() for more details"
      )
    else None

  override def onEndOfMedia(): Unit =
    super.onEndOfMedia()
    stateHub.send(PlayerStates.EndOfMedia)

  def close(): Unit =
    closeLine()
    stateHub.shutdown()
    timeUpdateHub.shutdown()

  def onPlaybackException(e: Exception): Unit = onEndOfMedia()

  def reset(): Unit =
    closeLine()
    stream = resetStream(stream)
    lineData = newLine(stream, stateHub.sink)

  /** Returns a stream of the media reset to its initial read position. Helper method for seeking.
    *
    * The default implementation merely calls `reset()` on the [[InputStream]] and returns the same
    * instance. If possible, override this method, close and open a new stream instead.
    *
    * @see
    *   [[BasicJavaSoundPlayer]]
    * @return
    *   a stream of the media reset to its initial read position
    */
  protected def resetStream(oldStream: InputStream): InputStream =
    oldStream.reset()
    oldStream

  private def closeLine(): Unit =
    active.set(false)
    lineData.close()
    startedFromMicros = 0L

  /** Closes the current line, starts from the beginning and then skips to the specified byte count.
    *
    * @param byteCount
    *   bytes to skip from start of track
    * @return
    *   actual bytes skipped from the beginning of the media
    */
  private def seekBytes(byteCount: StorageSize): StorageSize =
    // saves state
    val wasPlaying = lineData.state == PlayerStates.Started
    val wasMute = mute
    // seeks
    reset()
    val bytesSkipped = lineData.skip(byteCount.toBytes).bytes
    // restores state
    if wasPlaying then play()
    mute(wasMute)
    bytesSkipped

  private def startPlayback(): Unit =
    val changedToActive = active.compareAndSet(false, true)
    if changedToActive then
      audioLine.start()
      //    log.info(s"Starting playback of ${media.uri}")
      playThread = Some(Future(startPlayThread()).recover:
        // javazoom lib may throw at arbitrary playback moments
        case e: ArrayIndexOutOfBoundsException =>
          log.warn(e.getClass.getName, e)
          closeLine()
          onPlaybackException(e))

  private def startPlayThread(): Unit =
    val data = new Array[Byte](bufferSize)
    val END_OF_STREAM = -1
    var bytesRead = 0
    while bytesRead != END_OF_STREAM && isActive do
      // blocks until audio data is available
      bytesRead = lineData.read(data)
      if bytesRead != END_OF_STREAM then audioLine.write(data, 0, bytesRead)
      else
        // cleanup
        closeLine()
        // -1 bytes read means "end of stream has been reached"
        onEndOfMedia()

  def state = lineData.state

  private def tryMarkStream(): Unit =
    if stream.markSupported() then
      val markLimit = math.min(Integer.MAX_VALUE.toLong, 2 * media.size.toBytes).toInt
      stream.mark(markLimit)
      //      log.info(s"Mark limit is: $markLimit")
