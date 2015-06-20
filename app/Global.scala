import com.mle.musicpimp.Starter
import com.mle.musicpimp.json.JsonMessages
import com.mle.util.Log
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
    Starter.startServices()
  }

  override def onStop(app: Application): Unit = {
    Starter.stopServices()
    super.onStop(app)
  }

  /**
   * Run when a controller throws an exception.
   */
  override def onError(request: RequestHeader, ex: Throwable): Future[Result] =
    PimpContentController.pimpResult(
      html = super.onError(request, ex),
      json = Results.InternalServerError(JsonMessages.failure(s"${ex.getClass.getName}: ${ex.getMessage}")))(request)
}
