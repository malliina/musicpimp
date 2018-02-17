package com.malliina.musicpimp.cloud

import com.malliina.play.models.Password

object Constants extends Constants

trait Constants {
  // not a secret but avoids unintentional connections
  val pass = Password("pimp")
}
