package com.malliina.auth

import com.malliina.values.{Password, Username}

trait BasicCreds:
  def username: Username
  def password: Password

case class BasicUserPassCredentials(username: Username, password: Password) extends BasicCreds

case class RememberMeCredentials(username: Username, password: Password, rememberMe: Boolean)
  extends BasicCreds
