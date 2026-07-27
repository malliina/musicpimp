package com.malliina.musicpimp.cloud

import com.malliina.values.Literals.pass

object Constants extends Constants

trait Constants:
  // not a secret but avoids unintentional connections
  val pass = pass"pimp"
