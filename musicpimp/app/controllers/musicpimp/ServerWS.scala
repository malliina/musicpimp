package controllers.musicpimp

import org.apache.pekko.actor.Props
import org.apache.pekko.stream.{KillSwitches, Materializer}
import org.apache.pekko.stream.scaladsl.{Keep, Sink}
import com.malliina.musicpimp.audio.*
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.play.ActorExecution
import com.malliina.play.auth.Authenticator
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws.*
import io.circe.{Codec, Encoder}

/** Emits playback events to and accepts commands from listening clients.
  */
class ServerWS(
  player: MusicPlayer,
  val clouds: Clouds,
  auth: Authenticator[AuthedRequest],
  handler: PlaybackMessageHandler,
  ctx: ActorExecution
):
  implicit val mat: Materializer = ctx.materializer
  val serverMessages = player.allEvents
  val subscription = serverMessages
    .viaMat(KillSwitches.single)(Keep.right)
    .to(Sink.foreach: e =>
      sendToPimpcloud(e))
    .run()
  implicit val tm: Codec[TrackMeta] = TrackJson.format(clouds.cloudHost)
  val cloudWriter: Encoder[ServerMessage] =
    ServerMessage.jsonWriter(Encoder[TrackMeta])
  val sockets = new Sockets(auth, ctx):
    override def props(conf: ActorConfig[AuthedRequest]) =
      Props(new PlayerActor(player, handler, conf))

  private def sendToPimpcloud(message: ServerMessage) =
    clouds.sendIfConnected(cloudWriter(message))

  def openSocket = sockets.newSocket
