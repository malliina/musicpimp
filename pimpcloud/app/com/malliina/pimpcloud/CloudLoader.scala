package com.malliina.pimpcloud

import _root_.pimpcloud.Routes
import akka.stream.Materializer
import com.malliina.musicpimp.messaging.cloud.{PushResult, PushTask}
import com.malliina.musicpimp.messaging.{ProdPusher, Pusher}
import com.malliina.oauth.{GoogleOAuthCredentials, GoogleOAuthReader}
import com.malliina.pimpcloud.CloudComponents.log
import com.malliina.pimpcloud.ws.JoinedSockets
import com.malliina.play.ActorExecution
import com.malliina.play.app.DefaultApp
import controllers.pimpcloud._
import controllers.{Assets, AssetsComponents}
import play.api.ApplicationLoader.Context
import play.api.mvc.EssentialFilter
import play.api.{BuiltInComponentsFromContext, Logger, Mode}
import play.filters.HttpFiltersComponents
import play.filters.gzip.GzipFilter
import play.filters.headers.SecurityHeadersConfig

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.Try

case class AppConf(pusher: Pusher, googleCreds: GoogleOAuthCredentials, pimpAuth: (AdminOAuth, Materializer) => PimpAuth)

object AppConf {
  def dev = AppConf(
    NoPusher,
    Try(GoogleOAuthReader.load).getOrElse(GoogleOAuthCredentials("id", "secret", "scope")),
    (auth, mat) => new ProdAuth(new OAuthCtrl(auth, mat))
  )

  def prod = AppConf(
    ProdPusher.fromConf,
    GoogleOAuthReader.load,
    (auth, mat) => new ProdAuth(new OAuthCtrl(auth, mat))
  )

  def forMode(mode: Mode) =
    if (mode == Mode.Dev) dev
    else prod
}

class CloudLoader extends DefaultApp(ctx => new CloudComponents(ctx, AppConf.forMode(ctx.environment.mode)))

object NoPusher extends Pusher {
  override def push(pushTask: PushTask): Future[PushResult] =
    Future.successful(PushResult.empty)
}

object CloudComponents {
  private val log = Logger(getClass)
}

class CloudComponents(context: Context,
                      conf: AppConf)
  extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with AssetsComponents {

  val allowedCsp = Seq(
    "*.bootstrapcdn.com",
    "*.googleapis.com",
    "code.jquery.com",
    "use.fontawesome.com",
    "cdnjs.cloudflare.com"
  )
  val allowedEntry = allowedCsp.mkString(" ")

  val csp = s"default-src 'self' 'unsafe-inline' $allowedEntry; connect-src *; img-src 'self' data:;"
  override lazy val securityHeadersConfig = SecurityHeadersConfig(contentSecurityPolicy = Option(csp))

  implicit val ec: ExecutionContextExecutor = materializer.executionContext

  // Components
  override lazy val httpFilters: Seq[EssentialFilter] = Seq(securityHeadersFilter, new GzipFilter())

  val adminAuth = new AdminOAuth(defaultActionBuilder, conf.googleCreds)
  val pimpAuth: PimpAuth = conf.pimpAuth(adminAuth, materializer)

  lazy val tags = CloudTags.forApp(BuildInfo.frontName, environment.mode == Mode.Prod)
  lazy val ctx = ActorExecution(actorSystem, materializer)

  // Controllers
  lazy val joined = new JoinedSockets(pimpAuth, ctx, httpErrorHandler)
  lazy val cloudAuths = joined.auths
  lazy val push = new Push(controllerComponents, conf.pusher)
  lazy val p = Phones.forAuth(controllerComponents, tags, cloudAuths.phone, materializer)
  lazy val sc = ServersController.forAuth(controllerComponents, cloudAuths.server, materializer)
  lazy val l = new Logs(tags, pimpAuth, ctx, defaultActionBuilder)
  lazy val w = new Web(controllerComponents, tags, cloudAuths)
  lazy val as = new Assets(httpErrorHandler, assetsMetadata)
  lazy val router = new Routes(httpErrorHandler, p, w, push, joined, sc, l, adminAuth, joined.us, as)
  log info s"Started pimpcloud ${BuildInfo.version}"

  applicationLifecycle.addStopHook(() => Future.successful {
    adminAuth.validator.http.close()
  })
}
