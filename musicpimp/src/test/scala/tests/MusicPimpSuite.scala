package tests

import com.malliina.database.Conf
import com.malliina.musicpimp.app.{AppConf, InitOptions, LocalConf}
import com.typesafe.config.ConfigFactory
import play.api.Configuration

object TestOptions:
  val default =
    InitOptions(alarms = false, users = true, indexer = false, cloud = false)

object TestAppConf:
  val testConfFile = LocalConf.appDir.resolve("musicpimp-test.conf")
  val testConf = Configuration(ConfigFactory.parseFile(testConfFile.toFile))

class TestAppConf(conf: Conf) extends AppConf:
  override val databaseConf: Conf = conf
  override def close(): Unit = ()
