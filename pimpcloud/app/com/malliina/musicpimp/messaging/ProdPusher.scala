package com.malliina.musicpimp.messaging

import com.malliina.concurrent.Execution.cached
import com.malliina.musicpimp.messaging.ProdPusher.log
import com.malliina.musicpimp.messaging.cloud.{PushResult, PushTask}
import com.malliina.push.adm.ADMClient
import com.malliina.push.apns.APNSTokenConf
import com.malliina.push.gcm.GCMClient
import com.malliina.push.mpns.MPNSClient
import com.malliina.push.wns.{WNSClient, WNSCredentials}
import play.api.{Configuration, Logger}

import scala.concurrent.Future

class ProdPusher(
  apnsConf: APNSTokenConf,
  gcmApiKey: String,
  admCredentials: ADMCredentials,
  wnsCredentials: WNSCredentials
) extends Pusher {
  def this(conf: PushConf) = this(conf.apns, conf.gcmApiKey, conf.adm, conf.wns)

  // We push both to the sandboxed and prod environments in all cases,
  // because if we deploy from xcode we need sandboxed notifications
  val prodApnsHttp = APNSTokenHandler(apnsConf, isSandbox = false)
  val sandboxApnsHttp = APNSTokenHandler(apnsConf, isSandbox = true)
  val gcmHandler = new GCMHandler(new GCMClient(gcmApiKey))
  val admHandler = new ADMHandler(
    new ADMClient(admCredentials.clientId, admCredentials.clientSecret)
  )
  val mpnsHandler = new MPNSHandler(new MPNSClient)
  val wnsHandler = new WNSHandler(new WNSClient(wnsCredentials))

  def push(pushTask: PushTask): Future[PushResult] = {
    val prodApnsFuture = prodApnsHttp.push(pushTask.apns)
    val sandboxApnsFuture = sandboxApnsHttp.push(pushTask.apns)
    val gcmFuture = gcmHandler.push(pushTask.gcm)
    val admFuture = admHandler.push(pushTask.adm)
    val mpnsFuture = mpnsHandler.push(pushTask.mpns)
    val wnsFuture = wnsHandler.push(pushTask.wns)
    val r = for {
      apnsProd <- prodApnsFuture
      apnsSandbox <- sandboxApnsFuture
      gcm <- gcmFuture
      adm <- admFuture
      mpns <- mpnsFuture
      wns <- wnsFuture
    } yield {
      val labels = pushTask.labels.distinct
      val msg =
        if (labels.isEmpty) "Did not push, no push payloads."
        else s"Pushed to ${labels.mkString(", ")}"
      log info msg
      PushResult(apnsProd ++ apnsSandbox, gcm, adm, mpns, wns)
    }
    r.failed.foreach { t =>
      log.error(s"Push task failed.", t)
    }
    r
  }
}

object ProdPusher {
  private val log = Logger(getClass)

  def apply(conf: Configuration): ProdPusher = new ProdPusher(PushConf.orFail(conf))
}
