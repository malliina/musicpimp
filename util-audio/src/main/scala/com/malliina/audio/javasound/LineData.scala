package com.malliina.audio.javasound

import cats.effect.std.Dispatcher
import com.malliina.audio.PlayerStates
import com.malliina.audio.javasound.LineData.log
import fs2.concurrent.Topic
import org.slf4j.LoggerFactory

import java.io.InputStream
import javax.sound.sampled.*
import javax.sound.sampled.DataLine.Info

object LineData:
  private val log = LoggerFactory.getLogger(getClass)

  /** This factory method blocks as long as `stream` is empty, i.e. until an appropriate amount of
    * audio bytes has been made available to it.
    *
    * Therefore you must not, in the same thread, call this before bytes are made available to the
    * stream.
    */
  def fromStream[F[_]](
    stream: InputStream,
    sink: Topic[F, PlayerStates],
    d: Dispatcher[F]
  ) =
    new LineData(AudioSystem.getAudioInputStream(stream), sink, d)

class LineData[F[_]](
  inStream: AudioInputStream,
  sink: Topic[F, PlayerStates],
  d: Dispatcher[F]
):
  private val baseFormat = inStream.getFormat
  private val decodedFormat = toDecodedFormat(baseFormat)
  // this is read
  private val decodedIn = AudioSystem.getAudioInputStream(decodedFormat, inStream)
  // this is written to during playback
  val line = buildLine(decodedFormat)
  line.addLineListener((lineEvent: LineEvent) =>
    d.unsafeRunAndForget(sink.publish1(toPlayerEvent(lineEvent)))
  )
  line.open(decodedFormat)

  private def toPlayerEvent(lineEvent: LineEvent): PlayerStates =
    import PlayerStates.*
    import LineEvent.Type.*
    val eventType = lineEvent.getType
    if eventType == OPEN then Open
    else if eventType == CLOSE then Closed
    else if eventType == START then Started
    else if eventType == STOP then Stopped
    else Unknown

  def read(buffer: Array[Byte]): Int = decodedIn.read(buffer)

  def skip(bytes: Long): Long =
    val skipped = decodedIn.skip(bytes)
    log.debug(s"Attempted to skip $bytes bytes, skipped $skipped bytes")
    skipped

  def state: PlayerStates =
    import PlayerStates.*
    if line.isOpen then
      if line.isActive then Started
      else Stopped
    else Closed

  def close(): Unit =
    line.stop()
    line.flush()
    line.close()

  private def buildLine(format: AudioFormat): SourceDataLine =
    val info = new Info(classOf[SourceDataLine], format)
    val line = AudioSystem.getLine(info).asInstanceOf[SourceDataLine]
    // Add line listeners before opening the line.
    line.addLineListener((e: LineEvent) => log.debug(s"Line event: $e"))
    line

  private def toDecodedFormat(audioFormat: AudioFormat) = new AudioFormat(
    AudioFormat.Encoding.PCM_SIGNED,
    audioFormat.getSampleRate,
    16,
    audioFormat.getChannels,
    audioFormat.getChannels * 2,
    audioFormat.getSampleRate,
    false
  )
