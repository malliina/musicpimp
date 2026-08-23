package tests

import com.malliina.database.Conf
import com.malliina.musicpimp.app.{AppConf, InitOptions, LocalConf}

object TestOptions:
  val default =
    InitOptions(alarms = false, users = true, indexer = false, cloud = false)

object TestAppConf:
  val testConfFile = LocalConf.appDir.resolve("musicpimp-test.conf")
//  val testConf = Configuration(ConfigFactory.parseFile(testConfFile.toFile))

class TestAppConf(conf: Conf) extends AppConf:
  override val databaseConf: Conf = conf
  override def close(): Unit = ()
