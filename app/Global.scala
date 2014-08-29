import java.nio.file.Files

import com.mle.musicpimp.db.{Indexer, PimpDb}
import com.mle.musicpimp.log.PimpLog
import com.mle.musicpimp.scheduler.ScheduledPlaybackService
import com.mle.musicpimp.util.FileUtil
import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import com.mle.play.json.JsonMessages
import com.mle.util.{FileUtilities, Log}
import controllers.PimpContentController
import play.api.Application
import play.api.mvc._
import play.filters.gzip.GzipFilter

import scala.concurrent.Future


/**
 * @author Michael
 */
object Global extends WithFilters(new GzipFilter()) with Log {
  /**
   * Regarding logging:
   *
   * If I programmatically initialize a logback appender here, "Listening for HTTP on ..."
   * and subsequent log events are nowhere to be seen. Only the events that are written
   * before the "Listening" event, like the ones logged in this class, appear. What the
   * fuck?
   *
   * Adding an appender in XML receives the "Listening for HTTP on ..." and subsequent
   * events, but not preceding events. So XML is at least the better option.
   *
   * Also, this behavior only occurs if the app is started in production mode. Doing a
   * ~ run will cause the appender, if initialized here, to receive all events as
   * expected.
   *
   * The likely culprit is Play Framework's insistence on messing with the logback conf.
   */
  override def onStart(app: Application) {
    super.onStart(app)
    Files.createDirectories(FileUtil.pimpHomeDir)
    ScheduledPlaybackService.init()
    PimpDb.init()
    Future(Indexer.init())
    FileUtilities init "musicpimp"
    val version = com.mle.musicpimp.BuildInfo.version
    log info s"Started MusicPimp $version, base dir: ${FileUtilities.basePath}, user dir: ${FileUtilities.userDir}, log dir: ${PimpLog.logDir.toAbsolutePath}, app dir: ${FileUtil.pimpHomeDir}"
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] =
    PimpContentController.pimpResult(
      html = super.onError(request, ex),
      json = Results.InternalServerError(JsonMessages.failure(s"${ex.getClass.getName}: ${ex.getMessage}")))(request)
}
