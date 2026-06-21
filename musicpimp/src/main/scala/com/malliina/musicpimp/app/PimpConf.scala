package com.malliina.musicpimp.app

import cats.effect.Sync
import com.malliina.config.{ConfigError, ConfigNode}
import com.malliina.database.Conf
import com.malliina.http.FullUrl
import com.malliina.musicpimp.auth.SecretKey

import java.nio.file.Paths
import com.malliina.musicpimp.util.FileUtil
import com.malliina.values.{ErrorMessage, Password}

object PimpConf:
  val pimpConfFile = FileUtil.localPath("musicpimp.conf")
  val homeConf = Paths.get(sys.props("user.home"), ".musicpimp", "musicpimp.conf")
  val fileProps: Map[String, String] = FileUtil.props(pimpConfFile)

  val MySQLDriver = "com.mysql.cj.jdbc.Driver"
  val DefaultDriver = MySQLDriver

  def readConfFile(key: String): Option[String] = fileProps.get(key)

  def read(key: String) =
    sys.env
      .get(key)
      .orElse(sys.props.get(key))
      .orElse(readConfFile(key))
      .toRight(ErrorMessage(s"Key missing: '$key'."))

  def parseF[F[_]: Sync]: F[PimpConf] = Sync[F].fromEither(parse())

  def parse() =
    for
      pimp <- LocalConf.localConf.parse[ConfigNode]("musicpimp")
      conf <- parseConfig(pimp)
    yield conf

  private def parseConfig(c: ConfigNode): Either[ConfigError, PimpConf] =
    for
      secret <- c.parse[SecretKey]("secret")
      db <- c.parse[ConfigNode]("db")
      dbPass <- db.parse[Password]("pass")
    yield
      val appSecret =
        if secret == LocalConf.secretPlaceholder then
          LocalConf.readOrGenerateSecret(FileUtil.pimpHomeDir.resolve("play.secret.key"))
        else secret
      val dbConf = Conf(
        FullUrl("jdbc:mysql", "127.0.0.1:3306", "/musicpimp"),
        "musicpimp",
        dbPass,
        DefaultDriver,
        5,
        true,
        "flyway_schema_history"
      )
      PimpConf(secret, dbConf)

case class PimpConf(
  secret: SecretKey,
  db: Conf
)
