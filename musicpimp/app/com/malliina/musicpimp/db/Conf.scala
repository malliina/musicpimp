package com.malliina.musicpimp.db

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import play.api.{Configuration, Logger}

case class Conf(
  url: String,
  user: String,
  pass: String,
  driver: String,
  isMariaDb: Boolean = false)

object Conf {
  private val log = Logger(getClass)

  val UrlKey = "boat.db.url"
  val UserKey = "boat.db.user"
  val PassKey = "boat.db.pass"
  val DriverKey = "boat.db.driver"
  val MySQLDriver = "com.mysql.jdbc.Driver"
  val DefaultDriver = MySQLDriver

  def fromConf(conf: Configuration) = from(key => conf.getOptional[String](key))

  def from(readKey: String => Option[String]) = {
    def read(key: String) = readKey(key).toRight(s"Key missing: '$key'.")

    for {
      url <- read(UrlKey)
      user <- read(UserKey)
      pass <- read(PassKey)
    } yield Conf(url, user, pass, read(DriverKey).getOrElse(DefaultDriver), isMariaDb = false)
  }

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
