package com.malliina.musicpimp.messaging.cloud

import com.malliina.json.SharedPlayFormats
import com.malliina.push.adm.ADMToken
import com.malliina.push.android.AndroidMessage
import io.circe.Codec
import play.api.libs.json.{Format, Json, OFormat}

case class ADMRequest(tokens: Seq[ADMToken], message: AndroidMessage)
  extends PushRequest[ADMToken, AndroidMessage]

object ADMRequest {
//  implicit val token: Format[ADMToken] = SharedPlayFormats.from(ADMToken)
  implicit val json: Codec[ADMRequest] = Codec.derived[ADMRequest]
}
