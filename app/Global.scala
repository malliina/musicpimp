import com.mle.logbackrx.LogbackUtils
import com.mle.musicpimp.BuildInfo
import com.mle.musicpimp.log.{RxLogbackAppender, PimpLog}
import com.mle.musicpimp.scheduler.ScheduledPlaybackService
import com.mle.play.json.JsonMessages
import com.mle.util.{FileUtilities, Log}
import controllers.PimpContentController
import play.api.mvc.{Results, SimpleResult, RequestHeader, WithFilters}
import play.api.Application
import play.filters.gzip.GzipFilter
import scala.concurrent.Future


/**
 * @author Michael
 */
object Global extends WithFilters(new GzipFilter()) with Log {
  override def onStart(app: Application) {
    super.onStart(app)
    LogbackUtils.installAppender(RxLogbackAppender)
    ScheduledPlaybackService.init()
    FileUtilities init "musicpimp"
    val version = BuildInfo.version
    log info s"Started MusicPimp $version, base dir: ${FileUtilities.basePath}, user dir: ${FileUtilities.userDir}, log dir: ${PimpLog.logDir.toAbsolutePath}"
  }

  override def onError(request: RequestHeader, ex: Throwable): Future[SimpleResult] =
    PimpContentController.pimpResult(
      html = super.onError(request, ex),
      json = Results.InternalServerError(JsonMessages.failure(s"${ex.getClass.getName}: ${ex.getMessage}")))(request)
}
