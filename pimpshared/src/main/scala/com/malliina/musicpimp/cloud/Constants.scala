package com.malliina.musicpimp.cloud

import com.malliina.values.Password

object Constants extends Constants

trait Constants:
  // not a secret but avoids unintentional connections
  val pass = Password("pimp")
