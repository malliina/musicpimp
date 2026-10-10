package com.malliina.musicpimp.app

import cats.effect.Sync
import com.malliina.config.{ConfigError, ConfigNode}
import com.malliina.database.Conf
import com.malliina.http.UrlSyntax.url
import com.malliina.musicpimp.auth.SecretKey
import com.malliina.musicpimp.util.FileUtil
import com.malliina.values.{ErrorMessage, Password}

object PimpConf:
  private val pimpConfFile = FileUtil.localPath("musicpimp.conf")
  val homeConf = LocalConf.appDir.resolve("musicpimp.conf")
  private val fileProps: Map[String, String] = FileUtil.props(pimpConfFile)

  val MySQLDriver = "com.mysql.cj.jdbc.Driver"
  val DefaultDriver = MySQLDriver

  def readConfFile(key: String): Option[String] = fileProps.get(key)

  def read(key: String) =
    sys.env
      .get(key)
      .orElse(sys.props.get(key))
      .orElse(readConfFile(key))
      .toRight(ErrorMessage(s"Key missing: '$key'."))

  def parseF[F[_]: Sync]: F[PimpConf] = Sync[F].fromEither(parse(pass => defaultDatabaseConf(pass)))

  def parse(dbConf: Password => Conf) =
    for
      pimp <- LocalConf.localConf.parse[ConfigNode]("musicpimp")
      conf <- parseConfig(pimp, dbConf)
    yield conf

  private def parseConfig(
    c: ConfigNode,
    dbConf: Password => Conf
  ): Either[ConfigError, PimpConf] =
    val opts = if AppMode.fromBuild.isProd then InitOptions.prod else InitOptions.dev
    for
      secret <- c.parse[SecretKey]("secret")
      db <- c.parse[ConfigNode]("db")
      dbPass <- db.parse[Password]("pass")
    yield
      val appSecret =
        if secret == LocalConf.secretPlaceholder then
          LocalConf.readOrGenerateSecret(FileUtil.pimpHomeDir.resolve("musicpimp.secret"))
        else secret
      PimpConf(appSecret, dbConf(dbPass), opts)

  private def defaultDatabaseConf(password: Password): Conf =
    Conf(
      url"jdbc:mysql://127.0.0.1:3306/musicpimp",
      "musicpimp",
      password,
      DefaultDriver,
      5,
      Conf.DefaultMaxLifetime,
      Conf.DefaultKeepaliveTime,
      Conf.DefaultIdleTimeout,
      true
    )

case class PimpConf(
  secret: SecretKey,
  db: Conf,
  opts: InitOptions
)
