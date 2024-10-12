package com.malliina.pimpcloud.models

import com.malliina.values.Username
import io.circe.Encoder

case class PhoneRequest[W: Encoder](cmd: String, user: Username, body: W)
