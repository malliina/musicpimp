package com.malliina.musicpimp.app

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.security.SecureRandom
import _root_.musicpimp.Routes
import cats.effect.IO
import com.malliina.database.DoobieDatabase
import com.malliina.http.FullUrl
import com.malliina.concurrent.Execution.runtime
import com.malliina.logback.PimpAppender
import com.malliina.musicpimp.Starter
import com.malliina.musicpimp.audio.{MusicPlayer, PlaybackMessageHandler, StatsPlayer}
import com.malliina.musicpimp.auth.Auths
import com.malliina.musicpimp.cloud.{CloudSocket, Clouds, Deps}
import com.malliina.musicpimp.db.*
import com.malliina.musicpimp.html.PimpHtml
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.scheduler.ScheduledPlaybackService
import com.malliina.musicpimp.scheduler.json.JsonHandler
import com.malliina.musicpimp.util.FileUtil
import com.malliina.play.app.LoggingAppLoader
import com.malliina.play.auth.{Authenticator, RememberMe}
import com.malliina.play.controllers.AccountForms
import com.malliina.play.{ActorExecution, CookieAuthenticator, PimpAuthenticator}
import com.typesafe.config.ConfigFactory
import controllers.*
import controllers.musicpimp.*
import org.slf4j.LoggerFactory
import play.api.ApplicationLoader.Context
import play.api.http.{DefaultHttpErrorHandler, HttpErrorHandler}
import play.api.i18n.{I18nComponents, Lang}
import play.api.mvc.EssentialFilter
import play.api.{BuiltInComponentsFromContext, Configuration, Logger, Mode}
import play.filters.HttpFiltersComponents
import play.filters.gzip.GzipFilter

import scala.concurrent.{ExecutionContext, Future}

object LocalConf:
  val userHome = Paths.get(sys.props("user.home"))
  val localConfFile = userHome.resolve(".musicpimp/musicpimp.conf")
  val localConf = Configuration(ConfigFactory.parseFile(localConfFile.toFile))

case class InitOptions(
  alarms: Boolean = true,
  users: Boolean = true,
  indexer: Boolean = true,
  cloud: Boolean = true,
  cloudUri: FullUrl = CloudSocket.prodUri,
  useTray: Boolean = true
)

object InitOptions:
  val prod = InitOptions()
  val dev = InitOptions(
    alarms = false,
    users = true,
    indexer = true,
    cloud = false,
    useTray = false
  )

  // Ripped from Play's ApplicationSecretGenerator.scala
  def generateSecret(): String =
    val random = new SecureRandom()
    (1 to 64)
      .map: _ =>
        (random.nextInt(75) + 48).toChar
      .mkString
      .replaceAll("\\\\+", "/")

class PimpLoader(options: InitOptions) extends LoggingAppLoader[PimpComponents]:
  // Probably needed due to reference in application.conf to only the class name
  def this() = this(InitOptions.prod)

  override def createComponents(context: Context): PimpComponents =
    val env = context.environment
    val opts = if env.mode == Mode.Dev then InitOptions.dev else options
    PimpAppender.install()
    new PimpComponents(context, opts, LocalConf.localConf, conf => new ProdAppConf(conf))

trait AppConf:
  def databaseConf: Conf
  def close(): Unit

class ProdAppConf(c: Configuration) extends AppConf:
  private val log = Logger(getClass)

  var embedded: Option[EmbeddedMySQL] = None
  override val databaseConf: Conf = Conf
    .fromConfOrLegacy(c)
    .fold(
      err =>
        val e = EmbeddedMySQL.permanent
        embedded = Option(e)
        log.warn(s"Failed to load custom MySQL conf. Proceeding with embedded MariaDB. $err")
        e.conf
      ,
      identity
    )

  override def close(): Unit = embedded.foreach: db =>
    log.info(s"Closing embedded database...")
    db.stop()

object PimpComponents:
  private val log = LoggerFactory.getLogger(getClass)

class PimpComponents(
  context: Context,
  options: InitOptions,
  localConf: Configuration,
  init: Configuration => AppConf
) extends BuiltInComponentsFromContext(context)
  with HttpFiltersComponents
  with AssetsComponents
  with I18nComponents:
  import PimpComponents.log
  log.info(s"Local conf is '${LocalConf.localConfFile.toAbsolutePath}'.")
  val combinedConf = localConf.withFallback(context.initialConfiguration)
  val appSecretKey = "play.http.secret.key"
  val isFirstStart = combinedConf.get[String](appSecretKey) == "changeme"

  /** We distribute the same app package as a downloadable, but still want every user to have a
    * unique app secret. So, we generate and persist locally a random secret for each user on-demand
    * on app launch.
    */
  override lazy val configuration: Configuration =
    val initial = combinedConf
    val charset = StandardCharsets.UTF_8
    if environment.mode == Mode.Prod && isFirstStart then
      val dest: Path = FileUtil.pimpHomeDir.resolve("play.secret.key")
      val secret =
        if Files.exists(dest) && Files.isReadable(dest) then
          new String(Files.readAllBytes(dest), charset)
        else
          val secret = InitOptions.generateSecret()
          Option(dest.getParent).foreach(dir => Files.createDirectories(dir))
          Files.write(dest, secret.getBytes(charset))
          log.info(s"Generated random secret key and saved it to '$dest'...")
          secret
      Configuration(appSecretKey -> secret).withFallback(initial)
    else initial
  val appConf: AppConf = init(configuration)
  override lazy val httpFilters: Seq[EssentialFilter] = Seq(new GzipFilter())
  override lazy val httpErrorHandler: HttpErrorHandler =
    new DefaultHttpErrorHandler(
      environment,
      configuration,
      devContext.map(_.sourceMapper),
      Some(router)
    ) with PimpErrorHandling

  val player = MusicPlayer()
  val library = Library()(materializer)

  lazy val language = langs.availables.headOption getOrElse Lang.defaultLang
  lazy val messages = messagesApi.preferred(Seq(language))
  implicit val ec: ExecutionContext = materializer.executionContext
  // Services
  lazy val ctx = ActorExecution(actorSystem, materializer)
  val databaseConf = appConf.databaseConf
  val newDb = PimpMySQLDatabase.withMigrations(actorSystem, databaseConf)
  val doobieConf = com.malliina.database.Conf(
    databaseConf.url,
    databaseConf.user,
    databaseConf.pass,
    databaseConf.driver,
    maxPoolSize = 5,
    autoMigrate = false,
    schemaTable = "flyway_schema_history"
  )
  val (doobieResource, doobieFinalizer) =
    DoobieDatabase.default[IO](doobieConf).allocated.unsafeRunSync()
  val fullText = FullText(doobieResource)
  lazy val indexer = Indexer(library, DoobieIndexer(doobieResource), actorSystem.scheduler)
  val ps = NewPlaylist(newDb)
  val lib = DatabaseLibrary[IO](doobieResource, library)
  val userManager = DoobieUserManager.withUser(doobieResource).unsafeRunSync()
  lazy val rememberMe =
    new RememberMe(DoobieTokenStore(doobieResource), cookieSigner, httpConfiguration.secret)
  val stats = DatabaseStats[IO](doobieResource)
  lazy val statsPlayer = StatsPlayer(player, stats)
  lazy val auth = PimpAuthenticator(userManager, rememberMe, ec)
  lazy val handler = PlaybackMessageHandler(player, library, lib, statsPlayer)
  lazy val deps = Deps(ps, userManager, handler, lib, stats, schedules)
  lazy val clouds: Clouds =
    new Clouds(player, alarmHandler, deps, fullText, options.cloudUri, actorSystem.scheduler)
  lazy val tags = PimpHtml.forApp(environment.mode == Mode.Prod)
  lazy val auths = Auths(userManager, rememberMe)(ec)
  lazy val schedules = ScheduledPlaybackService(player, lib)
  lazy val alarmHandler = JsonHandler(player, schedules)
  lazy val compositeAuth = Authenticator.anyOne(
    CookieAuthenticator.default(auth)(ec),
    PimpAuthenticator.cookie(rememberMe)
  )
  lazy val webAuth = Secured.redirecting(compositeAuth)
  lazy val authDeps = AuthDeps(controllerComponents, webAuth, materializer)
  // Controllers
  lazy val ls = PimpLogs(ctx)
  lazy val lp = LogPage(tags, ls, authDeps)
  lazy val sws = ServerWS(player, clouds, auths.client, handler, ctx)
  lazy val webCtrl = Website(player, tags, sws, authDeps, stats)
  lazy val s = Search(indexer, auths.client, ctx)
  lazy val sp = SearchPage(tags, s, indexer, fullText, authDeps)
  lazy val r = Rest(player, library, lib, authDeps, handler, statsPlayer, httpErrorHandler)
  lazy val pl = Playlists(tags, ps, authDeps)
  lazy val settingsCtrl = SettingsController(tags, messages, library, indexer, authDeps)
  lazy val libCtrl = LibraryController(tags, lib, authDeps)
  lazy val alarms = Alarms(library, alarmHandler, tags, authDeps, messages)
  lazy val accs = AccountForms
  lazy val accounts = Accounts(tags, auth, authDeps, accs)
  lazy val cloud = Cloud(tags, clouds, authDeps)
  lazy val connect = ConnectController(authDeps)
  lazy val cloudWS = CloudWS(clouds, ctx)
  val starter = Starter(actorSystem)
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
    Future.successful:
      player.close()
      sws.subscription.shutdown()
      s.close()
      starter.stopServices(options, schedules, player)
      statsPlayer.close()
      Rest.close()
      clouds.disconnect("Stopping MusicPimp.")
      clouds.close()
      appConf.close()
      indexer.close()
      doobieFinalizer.unsafeRunSync()
  )
