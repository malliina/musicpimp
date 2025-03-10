package com.malliina.audio.javasound

import java.io.InputStream

import com.malliina.audio.PlayerStates
import com.malliina.audio.PlayerStates.PlayerState
import com.malliina.audio.javasound.LineData.log
import com.malliina.streams.EventSink
import javax.sound.sampled.DataLine.Info
import javax.sound.sampled.*
import org.slf4j.LoggerFactory

object LineData:
  private val log = LoggerFactory.getLogger(getClass)

  /** This factory method blocks as long as `stream` is empty, i.e. until an appropriate amount of
    * audio bytes has been made available to it.
    *
    * Therefore you must not, in the same thread, call this before bytes are made available to the
    * stream.
    */
  def fromStream(stream: InputStream, sink: EventSink[PlayerStates.PlayerState]) =
    new LineData(AudioSystem.getAudioInputStream(stream), sink)

class LineData(inStream: AudioInputStream, sink: EventSink[PlayerStates.PlayerState]):
  private val baseFormat = inStream.getFormat
  private val decodedFormat = toDecodedFormat(baseFormat)
  // this is read
  private val decodedIn = AudioSystem.getAudioInputStream(decodedFormat, inStream)
  // this is written to during playback
  val line = buildLine(decodedFormat)
  line.addLineListener((lineEvent: LineEvent) => sink.send(toPlayerEvent(lineEvent)))
  line.open(decodedFormat)

  def toPlayerEvent(lineEvent: LineEvent): PlayerState =
    import LineEvent.Type.*
    import PlayerStates.*
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

  def state: PlayerStates.Value =
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

  protected def toDecodedFormat(audioFormat: AudioFormat) = new AudioFormat(
    AudioFormat.Encoding.PCM_SIGNED,
    audioFormat.getSampleRate,
    16,
    audioFormat.getChannels,
    audioFormat.getChannels * 2,
    audioFormat.getSampleRate,
    false
  )
