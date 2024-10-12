package com.malliina.musicpimp.auth

import com.malliina.values.{Password, Username}
import org.apache.commons.codec.digest.DigestUtils

object Auth:
  def hash(user: Username, password: Password): String =
    hashStrings(user.name, password.pass)

  private def hashStrings(username: String, password: String): String =
    DigestUtils.md5Hex(username + ":" + password)
