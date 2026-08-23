package com.malliina.beam

import com.malliina.musicpimp.json.Target
import com.malliina.values.Username

case class PhoneClient[F[_]](user: Username, target: Target[F])
