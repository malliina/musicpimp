package com.malliina.pimpcloud

import cats.effect.Sync
import com.malliina.config.ConfigNode
import com.malliina.musicpimp.messaging.Pusher
import com.malliina.musicpimp.messaging.cloud.{PushResult, PushTask}
import com.malliina.util.AppLogger
import java.nio.file.Paths

object LocalConf:
  val userHome = Paths.get(sys.props("user.home"))
  val appDir = userHome.resolve(".pimpcloud")
  def local(file: String) = ConfigNode.default(appDir.resolve(file))
  val localConf = local("pimpcloud.conf")

class NoPusher[F[_]: Sync] extends Pusher[F]:
  override def push(pushTask: PushTask): F[PushResult] =
    Sync[F].pure(PushResult.empty)

object CloudComponents:
  private val log = AppLogger(getClass)

//class CloudComponents(context: Context, conf: AppConf)
//  extends BuiltInComponentsFromContext(context)
//  with HttpFiltersComponents
//  with AssetsComponents:
//
//  private val allowedCsp = Seq(
//    "*.bootstrapcdn.com",
//    "*.googleapis.com",
//    "code.jquery.com",
//    "use.fontawesome.com",
//    "cdnjs.cloudflare.com"
//  )
//  private val allowedEntry = allowedCsp.mkString(" ")
//
//  private val csp =
//    s"default-src 'self' 'unsafe-inline' 'unsafe-eval' $allowedEntry data:; connect-src *; img-src 'self' data:;"
//  override lazy val securityHeadersConfig = SecurityHeadersConfig(
//    contentSecurityPolicy = Option(csp)
//  )
//  override lazy val allowedHostsConfig = AllowedHostsConfig(Seq("cloud.musicpimp.org", "localhost"))
//
//  override lazy val configuration: Configuration =
//    LocalConf.localConf.withFallback(context.initialConfiguration)
//
//  val defaultHttpConf = HttpConfiguration.fromConfiguration(configuration, environment)
//  // Sets sameSite = None, otherwise the Google auth redirect will wipe out the session state
//  override lazy val httpConfiguration: HttpConfiguration =
//    defaultHttpConf.copy(
//      session = defaultHttpConf.session.copy(cookieName = "cloudSession", sameSite = None)
//    )
//
//  implicit val ec: ExecutionContextExecutor = materializer.executionContext
//
//  // Components
//  override lazy val httpFilters: Seq[EssentialFilter] = Seq(securityHeadersFilter, new GzipFilter())
//
//  val google = conf.conf(configuration)
//  private val adminAuth = new AdminOAuth(defaultActionBuilder, google)
//  val pimpAuth: PimpAuth = conf.pimpAuth(adminAuth, materializer)
//  val http = OkClient.default
//
//  lazy val tags = CloudTags.forApp(BuildInfo.frontName, environment.mode == Mode.Prod)
//  lazy val ctx = ActorExecution(actorSystem, materializer)
//
//  // Controllers
//  lazy val joined = new JoinedSockets(pimpAuth, ctx, httpErrorHandler)
//  lazy val cloudAuths = joined.auths
//  lazy val push = new Push(controllerComponents, conf.pusher(configuration, http))
//  lazy val p = Phones.forAuth(controllerComponents, tags, cloudAuths.phone, materializer)
//  lazy val sc = ServersController.forAuth(controllerComponents, cloudAuths.server, materializer)
//  lazy val l = new Logs(tags, pimpAuth, ctx, defaultActionBuilder)
//  lazy val w = new Web(controllerComponents, tags, cloudAuths)
//  lazy val as = new Assets(httpErrorHandler, assetsMetadata, environment)
//  lazy val router =
//    new Routes(httpErrorHandler, p, w, push, joined, sc, l, adminAuth, joined.us, as)
//  log.info(s"Started pimpcloud ${BuildInfo.version} with Google client ID '${google.clientId}'.")
//
//  applicationLifecycle.addStopHook(() =>
//    Future.successful:
//      http.close()
////      adminAuth.validator.http.close()
//  )
