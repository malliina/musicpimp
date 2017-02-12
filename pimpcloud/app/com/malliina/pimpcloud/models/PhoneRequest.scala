package com.malliina.pimpcloud.models

import play.api.libs.json.JsValue

case class PhoneRequest(cmd: String, body: JsValue)
