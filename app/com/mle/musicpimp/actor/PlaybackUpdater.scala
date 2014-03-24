package com.mle.musicpimp.actor

import akka.actor.Actor
import com.mle.actor.Messages.Stop
import com.mle.musicpimp.audio._
import com.mle.musicpimp.actor.Messages.{Restart, StartIfStopped, Shutdown}
import scala.Some
import com.mle.util.Log
import scala.concurrent.duration._
import com.mle.musicpimp.json.{JsonMessages, JsonSendah}

/**
 * @author Michael
 */
class PlaybackUpdater extends Actor {
  private var poller: Option[PimpPoller] = None

  def receive = {
    case Restart =>
      stop()
      start()
    case StartIfStopped =>
      if (poller.isEmpty) {
        start()
      }
    case Stop =>
      stop()
    case Shutdown =>
      stop()
      context.stop(self)
  }

  private def start() {
    MusicPlayer.underLying.foreach(player => {
      poller = Some(new PlaybackUpdatePoller)
    })
  }

  private def stop() {
    poller.map(_.close())
    poller = None
  }
}

/**
 * Server-side poller of media player. Responses pushed to clients over websockets.
 */
class PlaybackUpdatePoller()
  extends PimpPoller(1.seconds)
  with JsonSendah
  with Log {
  var previousPos = -1L
  def OnUpdate() {
    val pos = MusicPlayer.position
    log info s"Broadcasting: $pos"
    val posSeconds = pos.toSeconds
    if(posSeconds != previousPos){
      send(JsonMessages.timeUpdated(pos))
      previousPos = posSeconds
    }
  }
}