package com.malliina.musicpimp.messaging

import cats.effect.Sync
import cats.implicits.catsSyntaxApplicativeError
import cats.syntax.all.{toFlatMapOps, toFunctorOps}
import com.malliina.http.io.HttpClientF
import com.malliina.musicpimp.messaging.ProdPusher.log
import com.malliina.musicpimp.messaging.cloud.{PushResult, PushTask}
import com.malliina.push.apns.APNSTokenConf
import com.malliina.push.fcm.FCMClientF
import com.malliina.push.wns.WNSCredentials
import com.malliina.util.AppLogger

class ProdPusher[F[_]: Sync](
  apnsConf: APNSTokenConf,
  gcmApiKey: String,
  admCredentials: ADMCredentials,
  wnsCredentials: WNSCredentials,
  http: HttpClientF[F]
) extends Pusher[F]:
  val F = Sync[F]
  def this(conf: PushConf, http: HttpClientF[F]) =
    this(conf.apns, conf.gcmApiKey, conf.adm, conf.wns, http)

  // We push both to the sandboxed and prod environments in all cases,
  // because if we deploy from xcode we need sandboxed notifications
  val prodApnsHttp = APNSTokenHandler(apnsConf, http, isSandbox = false)
  val sandboxApnsHttp = APNSTokenHandler(apnsConf, http, isSandbox = true)
  val gcmHandler = new GCMHandler(FCMClientF(gcmApiKey, http))
//  val admHandler = new ADMHandler(new ADMClient(admCredentials.clientId, admCredentials.clientSecret))
//  val mpnsHandler = new MPNSHandler(new MPNSClient(http))
//  val wnsHandler = new WNSHandler(new WNSClient(wnsCredentials))

  def push(pushTask: PushTask): F[PushResult] =
    val prodApnsFuture = prodApnsHttp.push(pushTask.apns)
    val sandboxApnsFuture = sandboxApnsHttp.push(pushTask.apns)
    val gcmFuture = gcmHandler.push(pushTask.gcm)
//    val admFuture = admHandler.push(pushTask.adm)
//    val mpnsFuture = mpnsHandler.push(pushTask.mpns)
//    val wnsFuture = wnsHandler.push(pushTask.wns)
    val r = for
      apnsProd <- prodApnsFuture
      apnsSandbox <- sandboxApnsFuture
      gcm <- gcmFuture
//      adm <- admFuture
//      mpns <- mpnsFuture
//      wns <- wnsFuture
    yield
      val labels = pushTask.labels.distinct
      val msg =
        if labels.isEmpty then "Did not push, no push payloads."
        else s"Pushed to ${labels.mkString(", ")}"
      log.info(msg)
      PushResult(apnsProd ++ apnsSandbox, gcm, Nil, Nil, Nil)
    r.onError: t =>
      F.delay(log.error(s"Push task failed.", t))

object ProdPusher:
  private val log = AppLogger(getClass)

//  def apply(conf: Configuration, http: OkClient): ProdPusher =
//    new ProdPusher(PushConf.orFail(conf), http)
