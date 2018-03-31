package com.malliina.audio

import javax.sound.sampled.{LineEvent, LineListener}

import scala.concurrent.duration.Duration

object AudioImplicits {
  implicit def lineEvent2listener(onEvent: LineEvent => Unit): LineListener = new LineListener {
    def update(event: LineEvent): Unit = onEvent(event)
  }

  implicit class RichDuration(val t: Duration) {
    private val inSeconds = t.toSeconds.toInt

    private val secondsPart = inSeconds % 60
    private val minutesPart = (inSeconds - secondsPart) / 60 % 60
    private val hoursPart = inSeconds / 3600

    private val stringified =
      if (inSeconds >= 3600) {
        "%02d:%02d:%02d".format(hoursPart, minutesPart, secondsPart)
      } else {
        "%02d:%02d".format(minutesPart, secondsPart)
      }

    def readable = stringified
  }

}
