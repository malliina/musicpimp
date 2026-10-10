package com.malliina.musicpimp.db

import com.malliina.database.Conf
import com.malliina.http.FullUrl
import com.malliina.values.Password

object ConfBuilder:
  def makeConf(url: FullUrl, user: String, pass: Password, driver: String) =
    Conf(
      url,
      user,
      pass,
      driver,
      5,
      Conf.DefaultMaxLifetime,
      Conf.DefaultKeepaliveTime,
      Conf.DefaultIdleTimeout,
      autoMigrate = true,
      schemaTable = "flyway_schema_history"
    )
