package com.malliina.beam

import com.malliina.beam.AppComponents.log
import com.malliina.play.ActorExecution
import com.malliina.play.app.DefaultApp
import controllers.{AssetsComponents, Home}
import play.api.ApplicationLoader.Context
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
import play.api.{BuiltInComponentsFromContext, Logger}
import play.filters.HttpFiltersComponents
import play.filters.headers.SecurityHeadersConfig
import play.filters.hosts.AllowedHostsConfig
import router.Routes

import scala.concurrent.{ExecutionContext, Future}

class AppLoader extends DefaultApp(ctx => new AppComponents(ctx))

class AppComponents(context: Context)
  extends BuiltInComponentsFromContext(context)
    with AssetsComponents
    with HttpFiltersComponents
    with AhcWSComponents {

  val allowedCsp = Seq(
    "netdna.bootstrapcdn.com",
    "code.jquery.com",
    "cdnjs.cloudflare.com",
    "fonts.gstatic.com",
    "use.fontawesome.com",
    "fonts.googleapis.com",
    "ajax.googleapis.com"
  )
  val allowedEntry = allowedCsp.mkString(" ")

  val csp = s"default-src 'self' 'unsafe-inline' $allowedEntry; connect-src *; img-src 'self' data:;"
  override lazy val securityHeadersConfig = SecurityHeadersConfig(contentSecurityPolicy = Option(csp))
  override lazy val allowedHostsConfig = AllowedHostsConfig(Seq("localhost", "beam.musicpimp.org"))
  implicit val ec: ExecutionContext = materializer.executionContext
  // Services
  val disco = new DiscoGs(wsClient)
  val beams = Beams(ActorExecution(actorSystem, materializer))
  val conf = BeamConf(configuration)
  // Controllers
  val home = new Home(conf, beams, disco, materializer, httpErrorHandler, controllerComponents)
  override val router: Router = new Routes(httpErrorHandler, home, beams, assets)

  log info s"Started MusicBeamer endpoint. Advertising address: ${conf.host}:${conf.port}/${conf.sslPort}."

  applicationLifecycle.addStopHook(() => Future.successful {
    disco.close()
  })
}

object AppComponents {
  private val log = Logger(getClass)
}
