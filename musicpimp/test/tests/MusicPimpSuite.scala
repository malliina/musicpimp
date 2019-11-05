package tests

import com.malliina.musicpimp.app.{AppConf, InitOptions, PimpComponents}
import com.malliina.musicpimp.db.Conf
import org.scalatest.FunSuiteLike
import org.scalatestplus.play.{BaseOneAppPerSuite, FakeApplicationFactory}
import play.api.{Application, ApplicationLoader}

object TestOptions {
  val default =
    InitOptions(alarms = false, users = true, indexer = false, cloud = false)
}

class TestAppConf(conf: Conf) extends AppConf {
  override val databaseConf: Conf = conf
  override def close(): Unit = ()
}

class MusicPimpSuite(options: InitOptions)
  extends FunSuiteLike
  with EmbeddedMySQLSuite
  with BaseOneAppPerSuite
  with FakeApplicationFactory {
  lazy val components: PimpComponents = createComponents(TestAppLoader.createTestAppContext)

  override def fakeApplication(): Application = components.application

  def createComponents(context: ApplicationLoader.Context): PimpComponents =
    new PimpComponents(context, options, _ => new TestAppConf(embedded.conf))
}
