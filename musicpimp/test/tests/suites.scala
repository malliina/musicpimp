package tests

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.malliina.musicpimp.app.{AppConf, EmbeddedMySQL, InitOptions, PimpComponents}
import com.malliina.musicpimp.db.Conf
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}
import play.api.ApplicationLoader

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

trait EmbeddedMySQLSuite extends AsyncSuite {
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

object TestOptions {
  val default =
    InitOptions(alarms = false, users = true, indexer = false, cloud = false)
}

class TestAppConf(conf: Conf) extends AppConf {
  override val databaseConf: Conf = conf
  override def close(): Unit = ()
}

class MusicPimpSuite(options: InitOptions = TestOptions.default)
  extends EmbeddedMySQLSuite
  with OneAppPerSuite2[PimpComponents] {
  override def createComponents(context: ApplicationLoader.Context): PimpComponents =
    new PimpComponents(context, options, _ => new TestAppConf(embedded.conf))
}
