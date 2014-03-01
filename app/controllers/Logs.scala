package controllers

import views.html
import com.mle.musicpimp.log.PimpLog
import scala.util.{Success, Failure, Try}

/**
 *
 * @author mle
 */
trait Logs extends HtmlController {
  def logs = {
    tryToEither(loadLogEntries).fold(
      entries => html.logs(entries),
      msg => html.logs(Seq.empty, Some(msg)))
    navigate(html.logs(loadLogEntries))
  }

  def loadLogEntries = PimpLog.usingLog(_.toSeq.reverse.take(1000))

  def tryToEither[T](f: => T): Either[T, String] =
    Try(f) match {
      case Success(result) => Left(result)
      case Failure(t) => Right(t.getMessage)
    }
}
