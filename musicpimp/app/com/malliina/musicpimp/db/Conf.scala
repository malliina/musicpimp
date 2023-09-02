package com.malliina.musicpimp.db

import com.malliina.musicpimp.app.PimpConf
import com.malliina.values.ErrorMessage
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import play.api.{Configuration, Logger}

case class Conf(url: String, user: String, pass: String, driver: String)

object Conf {
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

  def from(readKey: String => Option[String]): Either[ErrorMessage, Conf] = {
    def read(key: String) = readKey(key).toRight(ErrorMessage(s"Key missing: '$key'."))

    for {
      url <- read(UrlKey)
      user <- read(UserKey)
      pass <- read(PassKey)
    } yield Conf(url, user, pass, read(DriverKey).getOrElse(DefaultDriver))
  }

  def read(key: String) = PimpConf.read(key)

  // Legacy
  def legacy(): Either[ErrorMessage, Conf] =
    for {
      url <- read("db_url")
      user <- read("db_user")
      pass <- read("db_pass")
    } yield Conf(url, user, pass, read("db_driver").getOrElse(MySQLDriver))

  def dataSource(conf: Conf): HikariDataSource = {
    val hikari = new HikariConfig()
    hikari.setDriverClassName(conf.driver)
    hikari.setJdbcUrl(conf.url)
    hikari.setUsername(conf.user)
    hikari.setPassword(conf.pass)
    log info s"Connecting to '${conf.url}'..."
    new HikariDataSource(hikari)
  }
}
