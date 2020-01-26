package com.malliina.musicpimp.app

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.security.SecureRandom

import _root_.musicpimp.Routes
import com.malliina.http.FullUrl
import com.malliina.musicpimp.Starter
import com.malliina.musicpimp.audio.{MusicPlayer, PlaybackMessageHandler, StatsPlayer}
import com.malliina.musicpimp.auth.Auths
import com.malliina.musicpimp.cloud.{CloudSocket, Clouds, Deps}
import com.malliina.musicpimp.db._
import com.malliina.musicpimp.html.PimpHtml
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.scheduler.ScheduledPlaybackService
import com.malliina.musicpimp.scheduler.json.JsonHandler
import com.malliina.musicpimp.util.FileUtil
import com.malliina.play.app.LoggingAppLoader
import com.malliina.play.auth.{Authenticator, RememberMe}
import com.malliina.play.controllers.AccountForms
import com.malliina.play.http.LogRequestFilter
import com.malliina.play.{ActorExecution, CookieAuthenticator, PimpAuthenticator}
import com.typesafe.config.ConfigFactory
import controllers._
import controllers.musicpimp._
import org.slf4j.LoggerFactory
import play.api.ApplicationLoader.Context
import play.api.http.{DefaultHttpErrorHandler, HttpErrorHandler}
import play.api.i18n.{I18nComponents, Lang}
import play.api.mvc.EssentialFilter
import play.api.{BuiltInComponentsFromContext, Configuration, Logger, Mode}
import play.filters.HttpFiltersComponents
import play.filters.gzip.GzipFilter

import scala.concurrent.{ExecutionContext, Future}

object LocalConf {
  val localConfFile =
    Paths.get(sys.props("user.home")).resolve(".musicpimp/musicpimp.conf")
  val localConf = Configuration(ConfigFactory.parseFile(localConfFile.toFile))
}

case class InitOptions(
  alarms: Boolean = true,
  users: Boolean = true,
  indexer: Boolean = true,
  cloud: Boolean = true,
  cloudUri: FullUrl = CloudSocket.prodUri,
  useTray: Boolean = true
)

object InitOptions {
  val prod = InitOptions()
  val dev = InitOptions(
    alarms = false,
    users = true,
    indexer = true,
    cloud = false,
    useTray = false
  )

  // Ripped from Play's ApplicationSecretGenerator.scala
  def generateSecret(): String = {
    val random = new SecureRandom()
    (1 to 64).map { _ =>
      (random.nextInt(75) + 48).toChar
    }.mkString.replaceAll("\\\\+", "/")
  }
}

class PimpLoader(options: InitOptions) extends LoggingAppLoader[PimpComponents] {
  // Probably needed due to reference in application.conf to only the class name
  def this() = this(InitOptions.prod)

  override def createComponents(context: Context): PimpComponents = {
    val env = context.environment
    val opts = if (env.mode == Mode.Dev) InitOptions.dev else options
    new PimpComponents(context, opts, conf => new ProdAppConf(conf))
  }
}

trait AppConf {
  def databaseConf: Conf
  def close(): Unit
}

class ProdAppConf(c: Configuration) extends AppConf {
  private val log = Logger(getClass)

  var embedded: Option[EmbeddedMySQL] = None
  override val databaseConf: Conf = Conf
    .prod(c)
    .fold(
      err => {
        val e = EmbeddedMySQL.permanent
        embedded = Option(e)
        log.warn(s"Failed to load custom MySQL conf. Proceeding with embedded MariaDB. $err")
        e.conf
      },
      identity
    )

  override def close(): Unit = embedded.foreach { db =>
    log.info(s"Closing embedded database...")
    db.stop()
  }
}

object PimpComponents {
  private val log = LoggerFactory.getLogger(getClass)
}

class PimpComponents(
  context: Context,
  options: InitOptions,
  init: Configuration => AppConf
) extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with AssetsComponents
  with I18nComponents {
  val combinedConf = LocalConf.localConf.withFallback(context.initialConfiguration)
  val appSecretKey = "play.http.secret.key"
  val isFirstStart = combinedConf.get[String](appSecretKey) == "changeme"

  /** We distribute the same app package as a downloadable, but still want every user to have a unique app secret. So,
    * we generate and persist locally a random secret for each user on-demand on app launch.
    */
  override lazy val configuration: Configuration = {
    val initial = combinedConf
    val charset = StandardCharsets.UTF_8
    if (environment.mode == Mode.Prod && isFirstStart) {
      val dest: Path = FileUtil.pimpHomeDir.resolve("play.secret.key")
      val secret =
        if (Files.exists(dest) && Files.isReadable(dest)) {
          new String(Files.readAllBytes(dest), charset)
        } else {
          val secret = InitOptions.generateSecret()
          Option(dest.getParent).foreach(dir => Files.createDirectories(dir))
          Files.write(dest, secret.getBytes(charset))
          PimpComponents.log.info(s"Generated random secret key and saved it to '$dest'...")
          secret
        }
      Configuration(appSecretKey -> secret).withFallback(initial)
    } else {
      initial
    }
  }
  val appConf: AppConf = init(configuration)
  override lazy val httpFilters: Seq[EssentialFilter] =
    Seq(LogRequestFilter(executionContext), new GzipFilter())
  override lazy val httpErrorHandler: HttpErrorHandler =
    new DefaultHttpErrorHandler(
      environment,
      configuration,
      devContext.map(_.sourceMapper),
      Some(router)
    ) with PimpErrorHandling

  val player = new MusicPlayer()
  val library = Library()(materializer)

  lazy val language = langs.availables.headOption getOrElse Lang.defaultLang
  lazy val messages = messagesApi.preferred(Seq(language))
  implicit val ec: ExecutionContext = materializer.executionContext
  // Services
  lazy val ctx = ActorExecution(actorSystem, materializer)
  val newDb = PimpMySQLDatabase.withMigrations(actorSystem, appConf.databaseConf)
  val fullText = FullText(newDb)
  lazy val indexer = new Indexer(library, NewIndexer(newDb), actorSystem.scheduler)
  val ps = NewPlaylist(newDb)
  val lib = NewDatabaseLibrary(newDb, library)
  val userManager = NewUserManager.withUser(newDb)
  lazy val rememberMe =
    new RememberMe(NewTokenStore(newDb), cookieSigner, httpConfiguration.secret)
  val stats = NewDatabaseStats(newDb)
  lazy val statsPlayer = new StatsPlayer(player, stats)
  lazy val auth = new PimpAuthenticator(userManager, rememberMe, ec)
  lazy val handler = new PlaybackMessageHandler(player, library, lib, statsPlayer)
  lazy val deps = Deps(ps, userManager, handler, lib, stats, schedules)
  lazy val clouds =
    new Clouds(player, alarmHandler, deps, fullText, options.cloudUri, actorSystem.scheduler)
  lazy val tags = PimpHtml.forApp(environment.mode == Mode.Prod)
  lazy val auths = new Auths(userManager, rememberMe)(ec)
  lazy val schedules = new ScheduledPlaybackService(player, lib)
  lazy val alarmHandler = new JsonHandler(player, schedules)
  lazy val compositeAuth = Authenticator.anyOne(
    CookieAuthenticator.default(auth)(ec),
    PimpAuthenticator.cookie(rememberMe)
  )
  lazy val webAuth = Secured.redirecting(compositeAuth)
  lazy val authDeps = AuthDeps(controllerComponents, webAuth, materializer)
  // Controllers
  lazy val ls = new PimpLogs(ctx)
  lazy val lp = new LogPage(tags, ls, authDeps)
  lazy val sws = new ServerWS(player, clouds, auths.client, handler, ctx)
  lazy val webCtrl = new Website(player, tags, sws, authDeps, stats)
  lazy val s = new Search(indexer, auths.client, ctx)
  lazy val sp = new SearchPage(tags, s, indexer, fullText, authDeps)
  lazy val r = new Rest(player, library, lib, authDeps, handler, statsPlayer, httpErrorHandler)
  lazy val pl = new Playlists(tags, ps, authDeps)
  lazy val settingsCtrl = new SettingsController(tags, messages, library, indexer, authDeps)
  lazy val libCtrl = new LibraryController(tags, lib, authDeps)
  lazy val alarms = new Alarms(library, alarmHandler, tags, authDeps, messages)
  lazy val accs = new AccountForms
  lazy val accounts = new Accounts(tags, auth, authDeps, accs)
  lazy val cloud = new Cloud(tags, clouds, authDeps)
  lazy val connect = new ConnectController(authDeps)
  lazy val cloudWS = new CloudWS(clouds, ctx)
  val starter = new Starter(actorSystem)
  starter.startServices(options, clouds, indexer, schedules, applicationLifecycle)
  val dummyForInit = statsPlayer

  lazy val router: Routes = new Routes(
    httpErrorHandler,
    libCtrl,
    webCtrl,
    settingsCtrl,
    connect,
    lp,
    cloud,
    alarms,
    accounts,
    r,
    pl,
    sp,
    s,
    sws,
    ls,
    cloudWS,
    assets
  )

  applicationLifecycle.addStopHook(() =>
    Future.successful {
      sws.subscription.shutdown()
      s.close()
      starter.stopServices(options, schedules, player)
      statsPlayer.close()
      Rest.close()
      clouds.disconnect("Stopping MusicPimp.")
      appConf.close()
    }
  )
}
