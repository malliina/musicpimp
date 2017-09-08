package controllers.musicpimp

import akka.actor.Props
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.play.ActorExecution
import com.malliina.play.auth.Authenticator
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws._

/** Emits playback events to and accepts commands from listening clients.
  */
class ServerWS(val clouds: Clouds,
               auth: Authenticator[AuthedRequest],
               handler: PlaybackMessageHandler,
               ctx: ActorExecution) {
  val serverMessages = MusicPlayer.allEvents
  val subscription = serverMessages.subscribe(event => sendToPimpcloud(event))
  val cloudWriter = ServerMessage.jsonWriter(TrackJson.format(clouds.cloudHost))
  val sockets = new Sockets(auth, ctx) {
    override def props(conf: ActorConfig[AuthedRequest]) =
      Props(new PlayerActor(MusicPlayer, handler, conf))
  }

  def sendToPimpcloud(message: ServerMessage) = {
    clouds.sendIfConnected(cloudWriter writes message)
  }

  def openSocket = sockets.newSocket
}
