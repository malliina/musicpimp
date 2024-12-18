package com.malliina.pimpcloud

import _root_.pimpcloud.Routes
import com.malliina.http.OkClient
import com.malliina.logback.PimpAppender
import org.apache.pekko.stream.Materializer
import com.malliina.musicpimp.messaging.cloud.{PushResult, PushTask}
import com.malliina.musicpimp.messaging.{ProdPusher, Pusher}
import com.malliina.oauth.GoogleOAuthCredentials
import com.malliina.pimpcloud.CloudComponents.log
import com.malliina.pimpcloud.ws.JoinedSockets
import com.malliina.play.ActorExecution
import com.typesafe.config.ConfigFactory
import controllers.pimpcloud.*
import controllers.{Assets, AssetsComponents}
import play.api.ApplicationLoader.Context
import play.api.http.HttpConfiguration
import play.api.mvc.EssentialFilter
import play.api.{Application, ApplicationLoader, BuiltInComponentsFromContext, Configuration, Logger, LoggerConfigurator, Mode}
import play.filters.HttpFiltersComponents
import play.filters.gzip.GzipFilter
import play.filters.headers.SecurityHeadersConfig
import play.filters.hosts.AllowedHostsConfig

import java.nio.file.Paths
import scala.concurrent.{ExecutionContextExecutor, Future}

object LocalConf:
  val userHome = Paths.get(sys.props("user.home"))
  val localConfFile = userHome.resolve(".pimpcloud/pimpcloud.conf")
  val localConf = Configuration(ConfigFactory.parseFile(localConfFile.toFile))

case class AppConf(
  pusher: (Configuration, OkClient) => Pusher,
  conf: Configuration => GoogleOAuthCredentials,
  pimpAuth: (AdminOAuth, Materializer) => PimpAuth
)

object AppConf:
  def dev = AppConf(
    (_, _) => NoPusher,
    conf =>
      GoogleOAuthCredentials(conf).toOption
        .getOrElse(GoogleOAuthCredentials("id", "secret", "scope")),
    (auth, mat) => new ProdAuth(new OAuthCtrl(auth, mat))
  )

  def prod = AppConf(
    (conf, http) => ProdPusher(conf, http),
    conf => GoogleOAuthCredentials(conf).toOption.get,
    (auth, mat) => new ProdAuth(new OAuthCtrl(auth, mat))
  )

  def forMode(mode: Mode) =
    if mode == Mode.Dev then dev
    else prod

class CloudLoader extends ApplicationLoader:
  override def load(context: Context): Application =
    val environment = context.environment
    LoggerConfigurator(environment.classLoader)
      .foreach(_.configure(environment, context.initialConfiguration, Map.empty))
    PimpAppender.install()
    new CloudComponents(context, AppConf.forMode(environment.mode)).application

object NoPusher extends Pusher:
  override def push(pushTask: PushTask): Future[PushResult] =
    Future.successful(PushResult.empty)

object CloudComponents:
  private val log = Logger(getClass)

class CloudComponents(context: Context, conf: AppConf)
  extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with AssetsComponents:

  private val allowedCsp = Seq(
    "*.bootstrapcdn.com",
    "*.googleapis.com",
    "code.jquery.com",
    "use.fontawesome.com",
    "cdnjs.cloudflare.com"
  )
  private val allowedEntry = allowedCsp.mkString(" ")

  private val csp =
    s"default-src 'self' 'unsafe-inline' 'unsafe-eval' $allowedEntry data:; connect-src *; img-src 'self' data:;"
  override lazy val securityHeadersConfig = SecurityHeadersConfig(
    contentSecurityPolicy = Option(csp)
  )
  override lazy val allowedHostsConfig = AllowedHostsConfig(Seq("cloud.musicpimp.org", "localhost"))

  override lazy val configuration: Configuration =
    LocalConf.localConf.withFallback(context.initialConfiguration)

  val defaultHttpConf = HttpConfiguration.fromConfiguration(configuration, environment)
  // Sets sameSite = None, otherwise the Google auth redirect will wipe out the session state
  override lazy val httpConfiguration: HttpConfiguration =
    defaultHttpConf.copy(
      session = defaultHttpConf.session.copy(cookieName = "cloudSession", sameSite = None)
    )

  implicit val ec: ExecutionContextExecutor = materializer.executionContext

  // Components
  override lazy val httpFilters: Seq[EssentialFilter] = Seq(securityHeadersFilter, new GzipFilter())

  val google = conf.conf(configuration)
  private val adminAuth = new AdminOAuth(defaultActionBuilder, google)
  val pimpAuth: PimpAuth = conf.pimpAuth(adminAuth, materializer)
  val http = OkClient.default

  lazy val tags = CloudTags.forApp(BuildInfo.frontName, environment.mode == Mode.Prod)
  lazy val ctx = ActorExecution(actorSystem, materializer)

  // Controllers
  lazy val joined = new JoinedSockets(pimpAuth, ctx, httpErrorHandler)
  lazy val cloudAuths = joined.auths
  lazy val push = new Push(controllerComponents, conf.pusher(configuration, http))
  lazy val p = Phones.forAuth(controllerComponents, tags, cloudAuths.phone, materializer)
  lazy val sc = ServersController.forAuth(controllerComponents, cloudAuths.server, materializer)
  lazy val l = new Logs(tags, pimpAuth, ctx, defaultActionBuilder)
  lazy val w = new Web(controllerComponents, tags, cloudAuths)
  lazy val as = new Assets(httpErrorHandler, assetsMetadata, environment)
  lazy val router =
    new Routes(httpErrorHandler, p, w, push, joined, sc, l, adminAuth, joined.us, as)
  log.info(s"Started pimpcloud ${BuildInfo.version} with Google client ID '${google.clientId}'.")

  applicationLifecycle.addStopHook(() =>
    Future.successful:
      http.close()
      adminAuth.validator.http.close()
  )
