package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.adm.ADMToken
import com.malliina.push.android.AndroidMessage
import io.circe.Codec

case class ADMRequest(tokens: Seq[ADMToken], message: AndroidMessage)
  extends PushRequest[ADMToken, AndroidMessage]

object ADMRequest:
  given Codec[ADMRequest] = Codec.derived[ADMRequest]
