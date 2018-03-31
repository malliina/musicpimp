package com.malliina.audio.javasound

import java.io.InputStream
import javax.sound.sampled.DataLine.Info
import javax.sound.sampled._

import com.malliina.audio.AudioImplicits._
import com.malliina.audio.PlayerStates
import com.malliina.audio.PlayerStates.PlayerState
import com.malliina.audio.javasound.LineData.log
import org.slf4j.LoggerFactory
import rx.lang.scala.Subject

object LineData {
  private val log = LoggerFactory.getLogger(getClass)

  /** This factory method blocks as long as `stream` is empty, i.e. until an appropriate amount
    * of audio bytes has been made available to it.
    *
    * Therefore you must not, in the same thread, call this before bytes are made available to the stream.
    *
    * @param stream
    * @return
    */
  def fromStream(stream: InputStream, subject: Subject[PlayerState]) =
    new LineData(AudioSystem.getAudioInputStream(stream), subject)
}

class LineData(inStream: AudioInputStream, subject: Subject[PlayerState]) {
  private val baseFormat = inStream.getFormat
  private val decodedFormat = toDecodedFormat(baseFormat)
  // this is read
  private val decodedIn = AudioSystem.getAudioInputStream(decodedFormat, inStream)
  // this is written to during playback
  val line = buildLine(decodedFormat)
  line.addLineListener((lineEvent: LineEvent) => subject.onNext(toPlayerEvent(lineEvent)))
  line open decodedFormat

  def toPlayerEvent(lineEvent: LineEvent): PlayerState = {
    import LineEvent.Type._

    import PlayerStates._
    val eventType = lineEvent.getType
    if (eventType == OPEN) Open
    else if (eventType == CLOSE) Closed
    else if (eventType == START) Started
    else if (eventType == STOP) Stopped
    else Unknown
  }

  def read(buffer: Array[Byte]) = decodedIn.read(buffer)

  def skip(bytes: Long) = {
    val skipped = decodedIn skip bytes
    log debug s"Attempted to skip $bytes bytes, skipped $skipped bytes"
    skipped
  }

  def state = {
    import PlayerStates._
    if (line.isOpen) {
      if (line.isActive) {
        Started
      } else {
        Stopped
      }
    } else {
      Closed
    }
  }

  def close() {
    line.stop()
    line.flush()
    line.close()
  }

  private def buildLine(format: AudioFormat): SourceDataLine = {
    val info = new Info(classOf[SourceDataLine], format)
    val line = AudioSystem.getLine(info).asInstanceOf[SourceDataLine]
    // Add line listeners before opening the line.
    line.addLineListener((e: LineEvent) => log debug s"Line event: $e")
    line
  }

  protected def toDecodedFormat(audioFormat: AudioFormat) = new AudioFormat(
    AudioFormat.Encoding.PCM_SIGNED,
    audioFormat.getSampleRate,
    16,
    audioFormat.getChannels,
    audioFormat.getChannels * 2,
    audioFormat.getSampleRate,
    false
  )
}