package tests

import com.malliina.musicpimp.app.LocalConf
import org.apache.pekko.actor.ActorSystem

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}

trait BaseSuite extends munit.FunSuite:
  val userHome = LocalConf.userHome

  def await[T](f: Future[T], duration: Duration = 40.seconds): T = Await.result(f, duration)

trait AsyncSuite extends BaseSuite:
  implicit val as: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = as.dispatcher

  override def afterAll(): Unit =
    await(as.terminate())
    super.afterAll()
