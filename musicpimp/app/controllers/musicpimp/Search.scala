package controllers.musicpimp

import org.apache.pekko.actor.Props
import org.apache.pekko.stream.scaladsl.{Keep, Sink}
import org.apache.pekko.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import com.malliina.musicpimp.db.Indexer
import com.malliina.musicpimp.models.{Refresh, SearchMessage, SearchStatus, Subscribe}
import com.malliina.play.ActorExecution
import com.malliina.play.auth.Authenticator
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws.{ActorConfig, ActorMeta, JsonActor, Sockets}
import controllers.musicpimp.Search.log
import io.circe.Json
import play.api.Logger

import scala.concurrent.ExecutionContextExecutor

object Search:
  private val log = Logger(getClass)
  val DefaultLimit = 1000

  def indexingSink(onNext: String => Unit) = Sink.foreach[String](s => onNext(s))

class Search(indexer: Indexer, auth: Authenticator[AuthedRequest], ctx: ActorExecution):
  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  implicit val mat: Materializer = ctx.materializer
  val loggingSink = Sink.foreach[Long]: fileCount =>
    log.info(s"Indexing... $fileCount files indexed...")
  val (killSwitch, done) = indexer.ongoing
    .viaMat(KillSwitches.single)(Keep.right)
    .toMat(Sink.foreach(s => s.runWith(loggingSink)))(Keep.both)
    .run()
  done.onComplete: _ =>
    log.info("Indexing complete.")
  val sockets = new Sockets(auth, ctx):
    override def props(conf: ActorConfig[AuthedRequest]) =
      Props(new SearchActor(indexer, conf))

  def openSocket = sockets.newSocket

  def close(): Unit =
    killSwitch.shutdown()

object SearchActor:
  private val log = Logger(getClass)

class SearchActor(indexer: Indexer, ctx: ActorMeta)(implicit mat: Materializer)
  extends JsonActor(ctx):
  import SearchActor.log
  var killSwitchOpt: Option[UniqueKillSwitch] = None
  val socketSink = Sink.foreach[Long]: fileCount =>
    send(s"Indexing... $fileCount files indexed so far...")

  override def preStart(): Unit =
    super.preStart()
    // WTF?
    send("")
    val (killSwitch, fut) = indexer.ongoing
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(
        Sink.foreach(s =>
          s.watchTermination()((_, d) =>
            d.onComplete: _ =>
              log.info("Indexing complete.")
              send("Indexing complete.")
          ).runWith(socketSink)
        )
      )(Keep.both)
      .run()
    killSwitchOpt = Option(killSwitch)
    fut.onComplete: _ =>
      send("Indexing complete.")

  override def onMessage(msg: Json): Unit =
    msg
      .as[SearchMessage]
      .map:
        case Refresh =>
          log.info("Refresh indexing...")
          send("Refresh indexing...")
          indexer.indexAndSave()
        case Subscribe =>
          ()
      .getOrElse:
        SearchActor.log warn s"Unknown message '$msg'."

  def send(message: String): Unit = sendOut(SearchStatus(message))

  override def postStop(): Unit =
    super.postStop()
    killSwitchOpt foreach { sub =>
      sub.shutdown()
    }
