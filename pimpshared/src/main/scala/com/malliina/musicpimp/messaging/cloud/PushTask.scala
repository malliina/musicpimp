package com.malliina.musicpimp.messaging.cloud

import io.circe.Codec

case class PushTask(
  apns: Seq[APNSPayload] = Nil,
  gcm: Seq[GCMPayload] = Nil,
  adm: Seq[ADMPayload] = Nil,
  mpns: Seq[MPNSPayload] = Nil,
  wns: Seq[WNSPayload] = Nil
) derives Codec.AsObject:
  def labels: Seq[String] = Seq(
    apns.map(_ => "APNS"),
    gcm.map(_ => "GCM"),
    adm.map(_ => "ADM"),
    mpns.map(_ => "MPNS"),
    wns.map(_ => "WNS")
  ).flatten
