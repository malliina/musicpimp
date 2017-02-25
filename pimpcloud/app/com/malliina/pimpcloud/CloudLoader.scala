package com.malliina.pimpcloud

import _root_.pimpcloud.Routes
import buildinfo.BuildInfo
import com.malliina.musicpimp.messaging.{ProdPusher, Pusher}
import com.malliina.oauth.{GoogleOAuthCredentials, GoogleOAuthReader}
import com.malliina.play.ActorExecution
import com.malliina.play.app.DefaultApp
import com.malliina.play.controllers.AccountForms
import controllers._
import controllers.pimpcloud._
import play.api.ApplicationLoader.Context
import play.api.mvc.EssentialFilter
import play.api.{BuiltInComponentsFromContext, Logger, Mode}
import play.filters.gzip.GzipFilter

class CloudLoader extends DefaultApp(new ProdComponents(_))

class ProdComponents(context: Context) extends CloudComponents(context, ProdPusher.fromConf, GoogleOAuthReader.load) {
  override lazy val pimpAuth: PimpAuth = new ProdAuth(new OAuthCtrl(adminAuth))
}

abstract class CloudComponents(context: Context,
                               pusher: Pusher,
                               oauthCreds: GoogleOAuthCredentials) extends BuiltInComponentsFromContext(context) {
  def pimpAuth: PimpAuth

  // Components
  override lazy val httpFilters: Seq[EssentialFilter] = Seq(new GzipFilter())
  val adminAuth = new AdminOAuth(oauthCreds, materializer)
  lazy val auth = new CloudAuth(materializer)
  val forms = new AccountForms

  lazy val tags = CloudTags.forApp(BuildInfo.frontName, environment.mode == Mode.Prod)
  lazy val ctx = ActorExecution(actorSystem, materializer)

  // Controllers
  lazy val us = new UsageStreaming(pimpAuth, ctx)
  lazy val joined = new JoinedSockets(us.mediator, ctx)
  lazy val s = joined.servers
  lazy val cloudAuths = joined.auths
  lazy val ps = joined.phones
  lazy val push = new Push(pusher)
  lazy val p = new Phones(tags, cloudAuths, auth)
  lazy val sc = new ServersController(cloudAuths, auth)
  lazy val aa = new AdminAuth(pimpAuth, adminAuth, tags, materializer)
  lazy val l = new Logs(tags, pimpAuth, ctx)
  lazy val w = new Web(tags, cloudAuths, materializer.executionContext, forms)
  lazy val as = new Assets(httpErrorHandler)
  lazy val router = new Routes(httpErrorHandler, p, w, push, joined, sc, l, adminAuth, aa, us, as)
  CloudComponents.log.info(s"Started pimpcloud ${BuildInfo.version}")
}

object CloudComponents {
  private val log = Logger(getClass)
}
