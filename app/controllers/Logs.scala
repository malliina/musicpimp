package controllers

import views.html
import com.mle.musicpimp.log.PimpLog
import scala.util.{Success, Failure, Try}
import java.io.FileNotFoundException
import java.nio.file.Files

/**
 *
 * @author mle
 */
trait Logs extends HtmlController {
  def logs = {
    val page =
      toEither(loadLogEntries).fold(
        entries => html.logs(entries),
        msg => html.logs(Seq.empty, Some(msg)))
    navigate(page)
  }

  def loadLogEntries = PimpLog.usingLog(_.toSeq.reverse.take(1000))

  def toEither[T](f: => T): Either[T, String] =
    try {
      Left(f)
    } catch {
      case fnfe: FileNotFoundException =>
        val path = PimpLog.logFile.toAbsolutePath
        Right(s"Cannot display logs, because the log file could not be found at: $path. You can change the log directory with the log.dir system property.")
      case t: Throwable =>
        Right(s"Cannot display logs. ${t.getMessage}")
    }
}
