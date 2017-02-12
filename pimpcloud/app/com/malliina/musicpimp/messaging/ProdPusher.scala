package com.malliina.musicpimp.messaging

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.push.adm.ADMClient
import com.malliina.push.apns.APNSClient
import com.malliina.push.gcm.GCMClient
import com.malliina.push.mpns.MPNSClient
import com.malliina.push.wns.{WNSClient, WNSCredentials}

import scala.concurrent.Future

class ProdPusher(apnsCredentials: APNSCredentials,
                 gcmApiKey: String,
                 admCredentials: ADMCredentials,
                 wnsCredentials: WNSCredentials) extends Pusher {
  def this(conf: PushConf) = this(conf.apns, conf.gcmApiKey, conf.adm, conf.wns)

  // We push both to the sandboxed and prod environments in all cases,
  // because if we deploy from xcode we need sandboxed notifications
  val prodApns = new APNSHandler(new APNSClient(
    apnsCredentials.keyStore,
    apnsCredentials.keyStorePass,
    isSandbox = false))
  val sandboxApns = new APNSHandler(new APNSClient(
    apnsCredentials.keyStore,
    apnsCredentials.keyStorePass,
    isSandbox = true))
  val gcmHandler = new GCMHandler(new GCMClient(gcmApiKey))
  val admHandler = new ADMHandler(new ADMClient(
    admCredentials.clientId,
    admCredentials.clientSecret))
  val mpnsHandler = new MPNSHandler(new MPNSClient)
  val wnsHandler = new WNSHandler(new WNSClient(wnsCredentials))

  def push(pushTask: PushTask): Future[PushResult] = {
    val prodApnsFuture = prodApns.push(pushTask.apns)
    val sandboxApnsFuture = sandboxApns.push(pushTask.apns)
    val gcmFuture = gcmHandler.push(pushTask.gcm)
    val admFuture = admHandler.push(pushTask.adm)
    val mpnsFuture = mpnsHandler.push(pushTask.mpns)
    val wnsFuture = wnsHandler.push(pushTask.wns)
    for {
      apnsProd <- prodApnsFuture
      apnsSandbox <- sandboxApnsFuture
      gcm <- gcmFuture
      adm <- admFuture
      mpns <- mpnsFuture
      wns <- wnsFuture
    } yield PushResult(apnsProd ++ apnsSandbox, gcm, adm, mpns, wns)
  }
}

object ProdPusher {
  def fromConf: ProdPusher = new ProdPusher(PushConfReader.load)
}
