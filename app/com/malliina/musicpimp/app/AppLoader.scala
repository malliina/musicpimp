package com.malliina.musicpimp.app

import com.malliina.musicpimp.Starter
import com.malliina.musicpimp.audio.{PlaybackMessageHandler, StatsPlayer}
import com.malliina.musicpimp.cloud.{Clouds, Deps}
import com.malliina.musicpimp.db._
import com.malliina.musicpimp.library.DatabaseLibrary
import com.malliina.musicpimp.stats.DatabaseStats
import com.malliina.musicpimp.tags.PimpTags
import com.malliina.play.PimpAuthenticator
import com.malliina.play.auth.RememberMe
import com.malliina.play.controllers.AccountForms
import controllers._
import play.api.ApplicationLoader.Context
import play.api.http.{DefaultHttpErrorHandler, HttpErrorHandler}
import play.api.i18n.{I18nComponents, Lang, Messages}
import play.api.mvc.EssentialFilter
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, LoggerConfigurator, Mode}
import play.filters.gzip.GzipFilter

import scala.concurrent.Future

import router.Routes

case class InitOptions(alarms: Boolean = true,
                       database: Boolean = true,
                       users: Boolean = true,
                       indexer: Boolean = true,
                       cloud: Boolean = true)

object InitOptions {
  val prod = InitOptions()
  val dev = InitOptions(alarms = false, database = false, users = false, indexer = false, cloud = false)
}

class PimpLoader(options: InitOptions) extends ApplicationLoader {
  def this() = this(InitOptions.prod)

  def load(context: Context) = {
    val env = context.environment
    LoggerConfigurator(env.classLoader)
      .foreach(_.configure(env))
    // faster app restart when in dev
    val opts = if(env.mode == Mode.Dev) InitOptions.dev else options
    new PimpComponents(context, opts).application
  }
}

class PimpComponents(context: Context, options: InitOptions)
  extends BuiltInComponentsFromContext(context)
  with I18nComponents {

  override lazy val httpFilters: Seq[EssentialFilter] = Seq(new GzipFilter())
  override lazy val httpErrorHandler: HttpErrorHandler =
    new DefaultHttpErrorHandler(environment, configuration, sourceMapper, Some(router)) with PimpErrorHandling

  lazy val language = langs.availables.headOption getOrElse Lang.defaultLang
  lazy val messages = Messages(language, messagesApi)

  // Services
  lazy val db = new PimpDb
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
  lazy val c = new Clouds(deps)
  lazy val tags = PimpTags.forApp(environment.mode == Mode.Prod)

  // Controllers
  lazy val ls = new PimpLogs(materializer)
  lazy val lp = new LogPage(tags, ls, auth, materializer)
  lazy val wp = new WebPlayer(auth, materializer)
  lazy val sws = new ServerWS(c, auth, handler, materializer)
  lazy val webCtrl = new Website(tags, wp, sws, auth, stats, materializer)
  lazy val s = new Search(indexer, auth, materializer)
  lazy val sp = new SearchPage(s, indexer, db, auth, materializer)
  lazy val r = new Rest(wp, auth, handler, statsPlayer, materializer)
  lazy val pl = new Playlists(ps, auth, materializer)
  lazy val settingsCtrl = new SettingsController(messages, indexer, auth, materializer)
  lazy val as = new Assets(httpErrorHandler)
  lazy val libCtrl = new LibraryController(tags, lib, auth, materializer)
  lazy val alarms = new Alarms(auth, messages, materializer)
  lazy val accs = new AccountForms
  lazy val accounts = new Accounts(auth, materializer, accs)
  lazy val cloud = new Cloud(c, auth, materializer)
  lazy val connect = new ConnectController(auth, materializer)
  lazy val assetsCtrl = new Assets(httpErrorHandler)

  Starter.startServices(options, c, db, indexer)
  val dummyForInit = statsPlayer

  lazy val router: Routes = new Routes(
    httpErrorHandler, libCtrl, webCtrl,
    settingsCtrl, connect, lp,
    cloud, accounts, r, pl,
    alarms, sp, s, sws,
    wp, ls, assetsCtrl)

  applicationLifecycle.addStopHook(() => Future.successful {
    sws.subscription.unsubscribe()
    s.subscription.unsubscribe()
    Starter.stopServices()
    statsPlayer.close()
    db.close()
  })
}
