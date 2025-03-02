package com.malliina.musicpimp.db

import com.malliina.musicpimp.app.PimpConf
import com.malliina.values.{ErrorMessage, Password}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import play.api.{Configuration, Logger}
import com.malliina.database.Conf
import com.malliina.http.FullUrl

object ConfBuilder:
  private val log = Logger(getClass)

  val UrlKey = "musicpimp.db.url"
  val UserKey = "musicpimp.db.user"
  val PassKey = "musicpimp.db.pass"
  val DriverKey = "musicpimp.db.driver"
  val MySQLDriver = "com.mysql.jdbc.Driver"
  val DefaultDriver = MySQLDriver

  def fromConfOrLegacy(conf: Configuration): Either[ErrorMessage, Conf] =
    fromConf(conf).orElse(legacy())

  def fromConf(conf: Configuration) = from(key => conf.getOptional[String](key))

  private def from(readKey: String => Option[String]): Either[ErrorMessage, Conf] =
    def read(key: String) = readKey(key).toRight(ErrorMessage(s"Key missing: '$key'."))

    for
      url <- read(UrlKey).flatMap(str => FullUrl.build(str))
      user <- read(UserKey)
      pass <- read(PassKey)
    yield makeConf(url, user, Password(pass), read(DriverKey).getOrElse(DefaultDriver))

  private def read(key: String) = PimpConf.read(key)

  // Legacy
  private def legacy(): Either[ErrorMessage, Conf] =
    for
      url <- read("db_url").flatMap(str => FullUrl.build(str))
      user <- read("db_user")
      pass <- read("db_pass")
    yield makeConf(url, user, Password(pass), read("db_driver").getOrElse(MySQLDriver))

  def makeConf(url: FullUrl, user: String, pass: Password, driver: String) =
    Conf(
      url,
      user,
      pass,
      driver,
      maxPoolSize = 5,
      autoMigrate = true,
      schemaTable = "flyway_schema_history"
    )

  def dataSource(conf: Conf): HikariDataSource =
    val hikari = new HikariConfig()
    hikari.setDriverClassName(conf.driver)
    hikari.setJdbcUrl(conf.url.url)
    hikari.setUsername(conf.user)
    hikari.setPassword(conf.pass.pass)
    log.info(s"Connecting to '${conf.url}'...")
    new HikariDataSource(hikari)
