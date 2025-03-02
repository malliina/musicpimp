package tests

import com.malliina.musicpimp.app.{AppConf, InitOptions, LocalConf, PimpComponents}
import com.malliina.database.Conf
import com.malliina.musicpimp.db.ConfBuilder
import com.typesafe.config.ConfigFactory
import play.api.{Configuration, Play}

object TestOptions:
  val default =
    InitOptions(alarms = false, users = true, indexer = false, cloud = false)

object TestAppConf:
  val testConfFile = LocalConf.userHome.resolve(".musicpimp/musicpimp-test.conf")
  val testConf = Configuration(ConfigFactory.parseFile(testConfFile.toFile))

class TestAppConf(conf: Conf) extends AppConf:
  override val databaseConf: Conf = conf
  override def close(): Unit = ()

trait MusicPimpSuite:
  self: munit.FunSuite =>
  val testApp: Fixture[PimpComponents] = new Fixture[PimpComponents]("pimp-app"):
    private var comps: PimpComponents = null
    override def apply() = comps

    override def beforeAll(): Unit =
      val dbConf =
        ConfBuilder
          .fromConf(TestAppConf.testConf)
          .fold(err => throw Exception(err.message), identity)
      comps = new PimpComponents(
        TestAppLoader.createTestAppContext,
        pimpOptions,
        TestAppConf.testConf,
        _ => new TestAppConf(dbConf)
      )
      Play.start(comps.application)

    override def afterAll(): Unit =
      Play.stop(comps.application)
  override def munitFixtures: Seq[Fixture[?]] = Seq(testApp)
  def components = testApp()
  def app = components.application
  def pimpOptions: InitOptions
