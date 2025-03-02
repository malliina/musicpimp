package com.malliina.musicpimp.app

import java.nio.file.{Files, Path}
import ch.vorburger.mariadb4j.{DB, DBConfiguration, DBConfigurationBuilder}
import com.malliina.database.Conf
import com.malliina.http.FullUrl
import com.malliina.musicpimp.cloud.Constants.pass
import com.malliina.musicpimp.db.ConfBuilder
import com.malliina.musicpimp.util.FileUtil
import com.malliina.values.Password

object EmbeddedMySQL:
  def temporary: EmbeddedMySQL =
    apply(Files.createTempDirectory("embedded"), temporary = true)
  def permanent: EmbeddedMySQL =
    apply(FileUtil.pimpHomeDir.resolve("embedded"), temporary = false)

  def apply(baseDir: Path, temporary: Boolean): EmbeddedMySQL =
    new EmbeddedMySQL(baseDir, temporary)

class EmbeddedMySQL(baseDir: Path, temporary: Boolean):
  private val dbConfig: DBConfiguration =
    DBConfigurationBuilder
      .newBuilder()
      .setDataDir(baseDir.resolve("data").toAbsolutePath.toString)
      .setLibDir(baseDir.resolve("lib").toAbsolutePath.toString)
      .setDeletingTemporaryBaseAndDataDirsOnShutdown(temporary)
      .build()
  lazy val db = DB.newEmbeddedDB(dbConfig)
  lazy val conf: Conf =
    db.start()
    val dbName = "pimptest"
    db.createDB(dbName)
    ConfBuilder.makeConf(
      FullUrl
        .build(dbConfig.getURL(dbName))
        .fold(err => throw IllegalArgumentException(err.message), identity),
      "root",
      Password(""),
      Conf.MySQLDriver
    )

  def stop(): Unit = db.stop()
