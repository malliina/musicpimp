package com.malliina.musicpimp.messaging

import com.malliina.push.adm.ADMToken
import com.malliina.push.android.AndroidMessage
import play.api.libs.json.Json

case class ADMRequest(tokens: Seq[ADMToken], message: AndroidMessage)
  extends PushRequest[ADMToken, AndroidMessage]

object ADMRequest {
  implicit val json = Json.format[ADMRequest]
}
