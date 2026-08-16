package com.malliina.pimpcloud.http4s

object Web:
  val serverFormKey = "server"

object AccountKeys extends AccountKeys

trait AccountKeys:
  val intendedUri = "intended_uri"
  val feedback = "feedback"
  val userFormKey = "username"
  val passFormKey = "password"
  val rememberMeKey = "remember"

  val oldPassKey = "oldPassword"
  val newPassKey = "newPassword"
  val newPassAgainKey = "newPasswordAgain"
