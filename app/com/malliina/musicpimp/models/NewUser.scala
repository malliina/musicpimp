package com.malliina.musicpimp.models

import com.malliina.play.models.{Password, Username}

case class NewUser(username: Username, pass: Password, passAgain: Password) {
  def passwordsMatch = pass == passAgain
}
