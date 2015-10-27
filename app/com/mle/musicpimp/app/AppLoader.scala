package com.mle.musicpimp.app

import com.mle.musicpimp.Starter
import com.mle.musicpimp.cloud.Clouds
import com.mle.musicpimp.db.{PimpDb, DatabasePlaylist}
import com.mle.musicpimp.library.PlaylistService
import controllers._
import play.api.ApplicationLoader.Context
import play.api.http.{DefaultHttpErrorHandler, HttpErrorHandler}
import play.api.i18n.{I18nComponents, Lang, Messages}
import play.api.mvc.EssentialFilter
import play.api.{ApplicationLoader, BuiltInComponentsFromContext, Logger}
import play.filters.gzip.GzipFilter
import router.Routes

import scala.concurrent.Future

case class InitOptions(alarms: Boolean = true,
                       database: Boolean = true,
                       users: Boolean = true,
                       indexer: Boolean = true,
                       cloud: Boolean = true)

/**
 * @author mle
 */
class PimpLoader(options: InitOptions) extends ApplicationLoader {
  def this() = this(InitOptions())

  def load(context: Context) = {
    Logger.configure(context.environment)
    new PimpComponents(context, options).application
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
  lazy val ps = new DatabasePlaylist(PimpDb)
  lazy val c = new Clouds(ps)

  // Controllers
  lazy val ls = new PimpLogs
  lazy val lp = new LogPage(ls)
  lazy val wp = new WebPlayer
  lazy val sws = new ServerWS(c)
  lazy val w = new Website(wp, sws)
  lazy val s = new Search
  lazy val sp = new SearchPage(s)
  lazy val r = new Rest(wp)
  lazy val pl = new Playlists(ps)
  lazy val sc = new SettingsController(messages)
  lazy val as = new Assets(httpErrorHandler)


  Starter.startServices(options, c)

  lazy val router: Routes = new Routes(
    httpErrorHandler, w, sc, lp,
    new Cloud(c), new Accounts, r, pl,
    new Alarms, sp, s, sws,
    wp, ls, new PimpAssets)

  applicationLifecycle.addStopHook(() => Future.successful {
    sws.subscription.unsubscribe()
    s.subscription.unsubscribe()
    Starter.stopServices()
  })
}
