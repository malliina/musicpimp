package com.malliina.pimpcloud.models

import com.malliina.values.Username
import play.api.libs.json._

case class PhoneRequest[W: Writes](cmd: String, user: Username, body: W)
