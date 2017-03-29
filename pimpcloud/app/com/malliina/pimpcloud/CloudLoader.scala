package com.malliina.pimpcloud

import _root_.pimpcloud.Routes
import buildinfo.BuildInfo
import com.malliina.musicpimp.messaging.{ProdPusher, Pusher}
import com.malliina.oauth.{GoogleOAuthCredentials, GoogleOAuthReader}
import com.malliina.pimpcloud.CloudComponents.log
import com.malliina.pimpcloud.ws.JoinedSockets
import com.malliina.play.ActorExecution
import com.malliina.play.app.DefaultApp
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
                               oauthCreds: GoogleOAuthCredentials)
  extends BuiltInComponentsFromContext(context) {

  def pimpAuth: PimpAuth

  implicit val ec = materializer.executionContext

  // Components
  override lazy val httpFilters: Seq[EssentialFilter] = Seq(new GzipFilter())
  val adminAuth = new AdminOAuth(oauthCreds, materializer)

  lazy val tags = CloudTags.forApp(BuildInfo.frontName, environment.mode == Mode.Prod)
  lazy val ctx = ActorExecution(actorSystem, materializer)

  // Controllers
  lazy val joined = new JoinedSockets(pimpAuth, ctx)
  lazy val cloudAuths = joined.auths
  lazy val push = new Push(pusher)
  lazy val p = Phones.forAuth(tags, cloudAuths.phone, materializer)
  lazy val sc = ServersController.forAuth(cloudAuths.server, materializer)
  lazy val aa = new AdminAuth(pimpAuth, adminAuth, tags)
  lazy val l = new Logs(tags, pimpAuth, ctx)
  lazy val w = new Web(tags, cloudAuths, ec)
  lazy val as = new Assets(httpErrorHandler)
  lazy val router = new Routes(httpErrorHandler, p, w, push, joined, sc, l, adminAuth, aa, joined.us, as)
  log info s"Started pimpcloud ${BuildInfo.version}"
}

object CloudComponents {
  private val log = Logger(getClass)
}
