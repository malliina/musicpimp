package com.malliina.musicpimp.messaging.cloud

import com.malliina.push.adm.ADMToken
import com.malliina.push.android.AndroidMessage
import com.malliina.push.apns.{APNSMessage, APNSToken}
import com.malliina.push.gcm.{GCMMessage, GCMToken}
import com.malliina.push.mpns.MPNSToken
import com.malliina.push.wns.WNSToken
import io.circe.Codec

case class APNSPayload(token: APNSToken, message: APNSMessage) derives Codec.AsObject

case class GCMPayload(token: GCMToken, message: GCMMessage) derives Codec.AsObject

case class ADMPayload(token: ADMToken, message: AndroidMessage) derives Codec.AsObject

case class MPNSPayload(token: MPNSToken, message: MPNSRequest) derives Codec.AsObject

case class WNSPayload(token: WNSToken, message: WNSRequest) derives Codec.AsObject
