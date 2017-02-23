package com.malliina.musicpimp.app

import _root_.musicpimp.Routes
import com.malliina.musicpimp.Starter
import com.malliina.musicpimp.audio.{PlaybackMessageHandler, StatsPlayer}
import com.malliina.musicpimp.cloud.{CloudSocket, Clouds, Deps}
import com.malliina.musicpimp.db._
import com.malliina.musicpimp.library.DatabaseLibrary
import com.malliina.musicpimp.models.FullUrl
import com.malliina.musicpimp.stats.DatabaseStats
import com.malliina.musicpimp.tags.PimpTags
import com.malliina.play.auth.RememberMe
import com.malliina.play.controllers.AccountForms
import com.malliina.play.{ActorExecution, PimpAuthenticator}
import controllers._
import controllers.musicpimp._
import play.api.ApplicationLoader.Context
import play.api.http.{DefaultHttpErrorHandler, HttpErrorHandler}
import play.api.i18n.{I18nComponents, Lang, Messages}
import play.api.mvc.EssentialFilter
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, LoggerConfigurator, Mode}
import play.filters.gzip.GzipFilter

import scala.concurrent.Future

case class InitOptions(alarms: Boolean = true,
                       database: Boolean = true,
                       users: Boolean = true,
                       indexer: Boolean = true,
                       cloud: Boolean = true,
                       cloudUri: FullUrl = CloudSocket.prodUri)

object InitOptions {
  val prod = InitOptions()
  val dev = InitOptions(alarms = false, database = false, users = false, indexer = false, cloud = false)
}

class PimpLoader(options: InitOptions) extends ApplicationLoader {
  // Probably needed due to reference in application.conf to only the class name
  def this() = this(InitOptions.prod)

  def load(context: Context) = {
    val env = context.environment
    LoggerConfigurator(env.classLoader)
      .foreach(_.configure(env))
    // faster app restart when in dev
    val opts = if (env.mode == Mode.Dev) InitOptions.dev else options
    new PimpComponents(context, opts, PimpDb.default()).application
  }
}

class PimpComponents(context: Context, options: InitOptions, db: PimpDb)
  extends BuiltInComponentsFromContext(context)
    with I18nComponents {

  override lazy val httpFilters: Seq[EssentialFilter] = Seq(new GzipFilter())
  override lazy val httpErrorHandler: HttpErrorHandler =
    new DefaultHttpErrorHandler(environment, configuration, sourceMapper, Some(router))
      with PimpErrorHandling

  lazy val language = langs.availables.headOption getOrElse Lang.defaultLang
  lazy val messages = Messages(language, messagesApi)

  // Services
  lazy val ctx = ActorExecution(actorSystem, materializer)
  lazy val indexer = new Indexer(db)
  lazy val ps = new DatabasePlaylist(db)
  lazy val lib = new DatabaseLibrary(db)
  lazy val userManager = new DatabaseUserManager(db)
  lazy val rememberMe = new RememberMe(new DatabaseTokenStore(db), cookieSigner)
  lazy val stats = new DatabaseStats(db)
  lazy val statsPlayer = new StatsPlayer(stats)
  lazy val auth = new PimpAuthenticator(userManager, rememberMe)
  lazy val handler = new PlaybackMessageHandler(lib, statsPlayer)
  lazy val deps = Deps(ps, db, userManager, handler, lib, stats)
  lazy val clouds = new Clouds(deps, options.cloudUri)
  lazy val tags = PimpTags.forApp(environment.mode == Mode.Prod)

  // Controllers
  lazy val security = new SecureBase(auth, materializer)
  lazy val ls = new PimpLogs(ctx)
  lazy val lp = new LogPage(tags, ls, auth, materializer)
  lazy val wp = new WebPlayer(security)
  lazy val sws = new ServerWS(clouds, security, handler)
  lazy val webCtrl = new Website(tags, wp, sws, auth, stats, materializer)
  lazy val s = new Search(indexer, security)
  lazy val sp = new SearchPage(tags, s, indexer, db, auth, materializer)
  lazy val r = new Rest(wp, auth, handler, statsPlayer, materializer)
  lazy val pl = new Playlists(tags, ps, auth, materializer)
  lazy val settingsCtrl = new SettingsController(tags, messages, indexer, auth, materializer)
  lazy val as = new Assets(httpErrorHandler)
  lazy val libCtrl = new LibraryController(tags, lib, auth, materializer)
  lazy val alarms = new Alarms(tags, auth, messages, materializer)
  lazy val accs = new AccountForms
  lazy val accounts = new Accounts(tags, auth, materializer, accs)
  lazy val cloud = new Cloud(tags, clouds, auth, materializer)
  lazy val connect = new ConnectController(auth, materializer)
  lazy val assetsCtrl = new Assets(httpErrorHandler)
  lazy val cloudWS = new CloudWS(clouds, ctx)

  Starter.startServices(options, clouds, db, indexer)
  val dummyForInit = statsPlayer

  lazy val router: Routes = new Routes(
    httpErrorHandler, libCtrl, webCtrl,
    settingsCtrl, connect, lp,
    cloud, accounts, r, pl,
    alarms, sp, s, sws,
    wp, ls, cloudWS, assetsCtrl)

  applicationLifecycle.addStopHook(() => Future.successful {
    sws.subscription.unsubscribe()
    s.subscription.unsubscribe()
    Starter.stopServices()
    statsPlayer.close()
    db.close()
  })
}
