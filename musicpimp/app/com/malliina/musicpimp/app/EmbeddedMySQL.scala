package com.malliina.musicpimp.app

import java.nio.file.{Files, Path}

import ch.vorburger.mariadb4j.{DB, DBConfiguration, DBConfigurationBuilder}
import com.malliina.musicpimp.db.Conf
import com.malliina.musicpimp.util.FileUtil

object EmbeddedMySQL {
  def temporary: EmbeddedMySQL =
    apply(Files.createTempDirectory("embedded"), temporary = true)
  def permanent: EmbeddedMySQL =
    apply(FileUtil.pimpHomeDir.resolve("embedded"), temporary = false)

  def apply(baseDir: Path, temporary: Boolean): EmbeddedMySQL =
    new EmbeddedMySQL(baseDir, temporary)
}

class EmbeddedMySQL(baseDir: Path, temporary: Boolean) {
  private val dbConfig: DBConfiguration =
    DBConfigurationBuilder
      .newBuilder()
      .setDataDir(baseDir.resolve("data").toAbsolutePath.toString)
      .setLibDir(baseDir.resolve("lib").toAbsolutePath.toString)
      .setDeletingTemporaryBaseAndDataDirsOnShutdown(temporary)
      .build()
  lazy val db = DB.newEmbeddedDB(dbConfig)
  lazy val conf: Conf = {
    db.start()
    val dbName = "pimptest"
    db.createDB(dbName)
    Conf(dbConfig.getURL(dbName), "root", "", Conf.MySQLDriver)
  }

  def stop(): Unit = db.stop()
}
