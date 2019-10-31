package tests

import java.nio.file.Paths

import com.malliina.musicpimp.db.PimpDb.DatabaseConf
import com.malliina.musicpimp.db.{DataMigrator, PimpDb}
import org.scalatest.FunSuite

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class MigrationTests extends FunSuite {

  ignore("restore dump") {
    val db = PimpDb.mysql(
      DatabaseConf(
        "jdbc:mariadb://10.0.0.1:3306/musicpimp",
        "change",
        "me",
        DatabaseConf.MySQLDriver
      )
    )
    val migrator = DataMigrator(db)
    val dumpFile = Paths.get("E:\\Stuff\\dump.json")
    val op = migrator.restoreDump(dumpFile, fromScratch = false)
    await(op)
  }

  def await[T](f: Future[T]): T = Await.result(f, 7200.seconds)
}
