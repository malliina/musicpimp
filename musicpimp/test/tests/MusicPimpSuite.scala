package tests

import com.malliina.musicpimp.app.{AppConf, EmbeddedMySQL, InitOptions, PimpComponents}
import com.malliina.musicpimp.db.Conf
import play.api.Play

object TestOptions {
  val default =
    InitOptions(alarms = false, users = true, indexer = false, cloud = false)
}

class TestAppConf(conf: Conf) extends AppConf {
  override val databaseConf: Conf = conf
  override def close(): Unit = ()
}

trait MusicPimpSuite { self: munit.FunSuite =>
  val testApp: Fixture[PimpComponents] = new Fixture[PimpComponents]("pimp-app") {
    private var comps: PimpComponents = null
    private var embedded: EmbeddedMySQL = null
    override def apply() = comps

    override def beforeAll(): Unit = {
      embedded = EmbeddedMySQL.temporary
      comps = new PimpComponents(
        TestAppLoader.createTestAppContext,
        TestOptions.default,
        _ => new TestAppConf(embedded.conf)
      )
      Play.start(comps.application)
    }

    override def afterAll(): Unit = {
      Play.stop(comps.application)
      embedded.stop()
    }
  }
  override def munitFixtures = Seq(testApp)
  def components = testApp()
  def app = components.application
}
