package com.malliina.musicpimp.app

import _root_.musicpimp.Routes
import com.malliina.http.FullUrl
import com.malliina.musicpimp.Starter
import com.malliina.musicpimp.audio.{PlaybackMessageHandler, StatsPlayer}
import com.malliina.musicpimp.auth.Auths
import com.malliina.musicpimp.cloud.{CloudSocket, Clouds, Deps}
import com.malliina.musicpimp.db._
import com.malliina.musicpimp.library.DatabaseLibrary
import com.malliina.musicpimp.stats.DatabaseStats
import com.malliina.musicpimp.tags.PimpHtml
import com.malliina.play.app.LoggingAppLoader
import com.malliina.play.auth.{Authenticator, RememberMe}
import com.malliina.play.controllers.AccountForms
import com.malliina.play.{ActorExecution, CookieAuthenticator, PimpAuthenticator}
import controllers._
import controllers.musicpimp._
import play.api.ApplicationLoader.Context
import play.api.http.{DefaultHttpErrorHandler, HttpErrorHandler}
import play.api.i18n.{I18nComponents, Lang}
import play.api.mvc.EssentialFilter
import play.api.{BuiltInComponentsFromContext, Mode}
import play.filters.HttpFiltersComponents
import play.filters.gzip.GzipFilter

import scala.concurrent.{ExecutionContext, Future}

case class InitOptions(alarms: Boolean = true,
                       database: Boolean = true,
                       users: Boolean = true,
                       indexer: Boolean = true,
                       cloud: Boolean = true,
                       cloudUri: FullUrl = CloudSocket.prodUri)

object InitOptions {
  val prod = InitOptions()
  val dev = InitOptions(alarms = false, database = true, users = true, indexer = false, cloud = false)
}

class PimpLoader(options: InitOptions) extends LoggingAppLoader[PimpComponents] {
  // Probably needed due to reference in application.conf to only the class name
  def this() = this(InitOptions.prod)

  override def createComponents(context: Context) = {
    val env = context.environment
    val opts = if (env.mode == Mode.Dev) InitOptions.dev else options
    new PimpComponents(context, opts, ec => PimpDb.default()(ec))
  }
}

class PimpComponents(context: Context, options: InitOptions, initDb: ExecutionContext => PimpDb)
  extends BuiltInComponentsFromContext(context)
    with HttpFiltersComponents
    with AssetsComponents
    with I18nComponents {

  override lazy val httpFilters: Seq[EssentialFilter] = Seq(new GzipFilter())
  override lazy val httpErrorHandler: HttpErrorHandler =
    new DefaultHttpErrorHandler(environment, configuration, sourceMapper, Some(router))
      with PimpErrorHandling

  lazy val language = langs.availables.headOption getOrElse Lang.defaultLang
  lazy val messages = messagesApi.preferred(Seq(language))
  implicit val ec = materializer.executionContext
  // Services
  lazy val ctx = ActorExecution(actorSystem, materializer)
  lazy val db = initDb(ec)
  lazy val indexer = new Indexer(db)
  lazy val ps = new DatabasePlaylist(db)
  lazy val lib = new DatabaseLibrary(db)
  lazy val userManager = new DatabaseUserManager(db)
  lazy val rememberMe = new RememberMe(new DatabaseTokenStore(db, ec), cookieSigner, httpConfiguration.secret)
  lazy val stats = new DatabaseStats(db)
  lazy val statsPlayer = new StatsPlayer(stats)
  lazy val auth = new PimpAuthenticator(userManager, rememberMe, ec)
  lazy val handler = new PlaybackMessageHandler(lib, statsPlayer)
  lazy val deps = Deps(ps, db, userManager, handler, lib, stats)
  lazy val clouds = new Clouds(deps, options.cloudUri)
  lazy val tags = PimpHtml.forApp(environment.mode == Mode.Prod)
  lazy val auths = new Auths(userManager, rememberMe)(ec)

  lazy val compositeAuth = Authenticator.anyOne(
    CookieAuthenticator.default(auth)(ec),
    PimpAuthenticator.cookie(rememberMe)
  )
  lazy val webAuth = Secured.redirecting(compositeAuth)
  lazy val authDeps = AuthDeps(controllerComponents, webAuth, materializer)

  // Controllers
  lazy val ls = new PimpLogs(ctx)
  lazy val lp = new LogPage(tags, ls, authDeps)
//  lazy val wp = new WebPlayer(ctx)
  lazy val sws = new ServerWS(clouds, auths.client, handler, ctx)
  lazy val webCtrl = new Website(tags, sws, authDeps, stats)
  lazy val s = new Search(indexer, auths.client, ctx)
  lazy val sp = new SearchPage(tags, s, indexer, db, authDeps)
  lazy val r = new Rest(authDeps, handler, statsPlayer, httpErrorHandler)
  lazy val pl = new Playlists(tags, ps, authDeps)
  lazy val settingsCtrl = new SettingsController(tags, messages, indexer, authDeps)
  lazy val libCtrl = new LibraryController(tags, lib, authDeps)
  lazy val alarms = new Alarms(tags, authDeps, messages)
  lazy val accs = new AccountForms
  lazy val accounts = new Accounts(tags, auth, authDeps, accs)
  lazy val cloud = new Cloud(tags, clouds, authDeps)
  lazy val connect = new ConnectController(authDeps)
  lazy val cloudWS = new CloudWS(clouds, ctx)

  Starter.startServices(options, clouds, db, indexer)
  val dummyForInit = statsPlayer

  lazy val router: Routes = new Routes(
    httpErrorHandler, libCtrl, webCtrl,
    settingsCtrl, connect, lp,
    cloud, accounts, r, pl,
    alarms, sp, s, sws,
    ls, cloudWS, assets)

  applicationLifecycle.addStopHook(() => Future.successful {
    sws.subscription.unsubscribe()
    s.subscription.unsubscribe()
    Starter.stopServices(options)
    statsPlayer.close()
    db.close()
  })
}
