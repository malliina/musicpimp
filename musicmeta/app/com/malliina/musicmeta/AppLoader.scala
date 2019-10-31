package com.malliina.musicmeta

import java.nio.file.Paths

import com.malliina.oauth.{DiscoGsOAuthCredentials, GoogleOAuthCredentials}
import com.malliina.play.ActorExecution
import com.malliina.play.app.DefaultApp
import com.typesafe.config.ConfigFactory
import controllers._
import play.api.ApplicationLoader.Context
import play.api.http.HttpConfiguration
import play.api.routing.Router
import play.api.{BuiltInComponentsFromContext, Configuration}
import play.filters.HttpFiltersComponents
import play.filters.headers.SecurityHeadersConfig
import play.filters.hosts.AllowedHostsConfig
import router.Routes

import scala.concurrent.Future

object LocalConf {
  val localConfFile = Paths.get(sys.props("user.home")).resolve(".musicmeta/musicmeta.conf")
  val localConf = Configuration(ConfigFactory.parseFile(localConfFile.toFile))
}

class AppLoader extends DefaultApp(AppComponents.prod)

object AppComponents {
  def prod(ctx: Context) = new AppComponents(
    ctx,
    c => DiscoGsOAuthCredentials(c).fold(err => throw new Exception(err.message), identity),
    c => GoogleOAuthCredentials(c).fold(err => throw new Exception(err.message), identity)
  )
}

class AppComponents(
  context: Context,
  disco: Configuration => DiscoGsOAuthCredentials,
  google: Configuration => GoogleOAuthCredentials
) extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with AssetsComponents {
  override val configuration: Configuration = context.initialConfiguration ++ LocalConf.localConf
  val allowedCsp = Seq(
    "*.musicpimp.org",
    "*.bootstrapcdn.com",
    "*.googleapis.com",
    "code.jquery.com",
    "use.fontawesome.com",
    "cdnjs.cloudflare.com"
  )
  val allowedEntry = allowedCsp.mkString(" ")

  val csp =
    s"default-src 'self' 'unsafe-inline' 'unsafe-eval' $allowedEntry; connect-src *; img-src 'self' data:;"
  override lazy val securityHeadersConfig = SecurityHeadersConfig(
    contentSecurityPolicy = Option(csp)
  )
  override lazy val allowedHostsConfig = AllowedHostsConfig(Seq("localhost", "api.musicpimp.org"))

  val defaultHttpConf = HttpConfiguration.fromConfiguration(configuration, environment)
  // Sets sameSite = None, otherwise the Google auth redirect will wipe out the session state
  override lazy val httpConfiguration =
    defaultHttpConf.copy(
      session = defaultHttpConf.session.copy(cookieName = "metaSession", sameSite = None)
    )

  lazy val oauthControl =
    new MetaOAuthControl(controllerComponents.actionBuilder, google(configuration))
  lazy val exec = ActorExecution(actorSystem, materializer)
  lazy val oauth = MetaOAuth(
    "username",
    MetaHtml(BuildInfo.frontName, environment.mode),
    defaultActionBuilder,
    exec
  )
  lazy val covers = new Covers(oauth, disco(configuration), controllerComponents)
  lazy val metaAssets = new MetaAssets(assets)
  override val router: Router =
    new Routes(httpErrorHandler, oauth, oauthControl, covers, metaAssets)

  applicationLifecycle.addStopHook { () =>
    Future.successful(oauthControl.http.close())
  }
}
