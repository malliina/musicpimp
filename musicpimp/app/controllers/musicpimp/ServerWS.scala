package controllers.musicpimp

import akka.actor.Props
import akka.stream.KillSwitches
import akka.stream.scaladsl.{Keep, Sink}
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.play.ActorExecution
import com.malliina.play.auth.Authenticator
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws._

/** Emits playback events to and accepts commands from listening clients.
  */
class ServerWS(
  player: MusicPlayer,
  val clouds: Clouds,
  auth: Authenticator[AuthedRequest],
  handler: PlaybackMessageHandler,
  ctx: ActorExecution
) {
  implicit val mat = ctx.materializer
  val serverMessages = player.allEvents
  val subscription = serverMessages
    .viaMat(KillSwitches.single)(Keep.right)
    .to(Sink.foreach { e =>
      sendToPimpcloud(e)
    })
    .run()
  val cloudWriter = ServerMessage.jsonWriter(TrackJson.format(clouds.cloudHost))
  val sockets = new Sockets(auth, ctx) {
    override def props(conf: ActorConfig[AuthedRequest]) =
      Props(new PlayerActor(player, handler, conf))
  }

  def sendToPimpcloud(message: ServerMessage) = {
    clouds.sendIfConnected(cloudWriter writes message)
  }

  def openSocket = sockets.newSocket
}
