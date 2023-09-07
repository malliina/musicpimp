package com.malliina.musicpimp.messaging.cloud

import play.api.libs.json.{Json, OFormat}

case class PushTask(apns: Seq[APNSPayload] = Nil,
                    gcm: Seq[GCMPayload] = Nil,
                    adm: Seq[ADMPayload] = Nil,
                    mpns: Seq[MPNSPayload] = Nil,
                    wns: Seq[WNSPayload] = Nil) {
  def labels: Seq[String] = Seq(
    apns.map(_ => "APNS"),
    gcm.map(_ => "GCM"),
    adm.map(_ => "ADM"),
    mpns.map(_ => "MPNS"),
    wns.map(_ => "WNS")
  ).flatten
}

object PushTask {
  implicit val json: OFormat[PushTask] = Json.format[PushTask]
}
