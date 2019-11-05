package tests

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.malliina.musicpimp.app.EmbeddedMySQL
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}

trait BaseSuite extends FunSuiteLike {
  val userHome = Paths.get(sys.props("user.home"))

  def await[T](f: Future[T], duration: Duration = 40.seconds): T = Await.result(f, duration)
}

trait AsyncSuite extends BaseSuite with BeforeAndAfterAll {
  implicit val as: ActorSystem = ActorSystem()
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = mat.executionContext

  override protected def afterAll(): Unit = {
    await(as.terminate())
    super.afterAll()
  }
}

trait EmbeddedMySQLSuite extends FunSuiteLike with BeforeAndAfterAll {
  val embedded = EmbeddedMySQL.temporary

  // This hack ensures that beforeAll and afterAll is run even when all tests are ignored,
  // ensuring resources are cleaned up in all situations.
  test("database lifecycle") {
    assert(1 === 1)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    val c = embedded.conf
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    embedded.stop()
  }
}
