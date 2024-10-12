package tests

import java.nio.file.Paths

import org.apache.pekko.actor.ActorSystem

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}

trait BaseSuite extends munit.FunSuite:
  val userHome = Paths.get(sys.props("user.home"))

  def await[T](f: Future[T], duration: Duration = 40.seconds): T = Await.result(f, duration)

trait AsyncSuite extends BaseSuite:
  implicit val as: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContext = as.dispatcher

  override def afterAll(): Unit =
    await(as.terminate())
    super.afterAll()
