package com.malliina.musicpimp.messaging

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.messaging.TokenService.log
import com.malliina.musicpimp.messaging.adm.{AmazonDevices, ADMBuilder}
import com.malliina.musicpimp.messaging.apns.{APNSDevices, APNSBuilder}
import com.malliina.musicpimp.messaging.cloud.{APNSHttpResult, PushTask}
import com.malliina.musicpimp.messaging.gcm.{GCMBuilder, GoogleDevices}
import com.malliina.musicpimp.messaging.mpns.{MPNSBuilder, PushUrls}
import com.malliina.push.apns.Unregistered
import play.api.Logger

object TokenService {
  private val log = Logger(getClass)

  val default = new TokenService
}

class TokenService {
  val apnsClient = new APNSBuilder()
  val mpns = new MPNSBuilder()
  val gcm = new GCMBuilder()
  val adm = new ADMBuilder()

  def sendNotifications(): Unit = {
    val apns = APNSDevices.get().map(apnsClient.buildRequest)
    val toasts = PushUrls.get().map(mpns.buildRequest)
    val gcms = GoogleDevices.get().map(gcm.buildRequest)
    val adms = AmazonDevices.get().map(adm.buildRequest)
    val task = PushTask(apns, gcms, adms, toasts, Nil)
    val messages = apns ++ toasts ++ gcms ++ adms
    if (messages.isEmpty) {
      log.info(s"No push notification URLs are active, so no push notifications were sent.")
    } else {
      CloudPushClient.default.push(task).map { response =>
        log info s"Sent ${messages.size} notifications."
        removeUnregistered(response.apns)
      }.recoverAll { t =>
        log.warn(s"Unable to send all notifications.", t)
      }
    }
  }

  def removeUnregistered(rs: Seq[APNSHttpResult]): Unit = {
    val removable = rs.filter(_.error.contains(Unregistered)).map(_.token)
    APNSDevices.removeAll(removable).foreach { d =>
      log.info(s"Removed unregistered token '${d.id}'.")
    }
  }
}
