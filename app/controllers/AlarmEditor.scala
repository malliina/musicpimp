package controllers

import akka.stream.Materializer
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.scheduler._
import com.malliina.musicpimp.scheduler.web.SchedulerStrings
import com.malliina.play.Authenticator
import com.malliina.play.http.AuthRequest
import controllers.AlarmEditor.log
import play.api.Logger
import play.api.data.Forms._
import play.api.data.{Form, Forms}
import play.api.http.Writeable
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request, Result}
import views.html

class AlarmEditor(auth: Authenticator, messages: Messages, mat: Materializer)
  extends Secured(auth, mat)
    with SchedulerStrings {

  private val clockForm: Form[ClockPlayback] = Form(mapping(
    ID -> optional(text),
    HOURS -> number(min = 0, max = 24),
    MINUTES -> number(min = 0, max = 59),
    DAYS -> Forms.seq(text).verifying("Must select at least one day", _.nonEmpty),
    TRACK -> nonEmptyText,
    TRACK_ID -> nonEmptyText,
    ENABLED -> optional(text)
  )((id, hours, minutes, ds, _, trackID, enabledOpt) => {
    //    log.info(s"submitted track with ID: $trackID")
    // converts submitted form data to a case class
    val (days, enabled, job) = parseDaysEnabledAndJob(ds, enabledOpt, trackID)
    val s = ClockSchedule(hours, minutes, days)
    ClockPlayback(id, job, s, enabled)
  })(ap => {
    // decomposes a case class to an optional tuple of its constituent values
    val i = ap.when
    val trackID = ap.job.track
    val trackTitle = Library.findMeta(trackID).fold(trackID)(_.title)
    Some((ap.id, i.hour, i.minute, i.days.map(_.shortName), trackTitle, trackID, Some(if (ap.enabled) ON else OFF)))
  }))

  private def parseDaysEnabledAndJob(days: Seq[String], enabledOpt: Option[String], track: String): (Seq[WeekDay], Boolean, PlaybackJob) = {
    val weekDays = days.flatMap(WeekDay.withShortName)
    val enabled = enabledOpt.contains(SchedulerStrings.ON)
    val job = PlaybackJob(track)
    (weekDays, enabled, job)
  }

  def newAlarm = clockAction(clockForm)

  def editAlarm(id: String, fb: Option[String] = None) = {
    ScheduledPlaybackService.find(id)
      .map(clockForm.fill)
      .fold(ifEmpty = PimpAction(NotFound(s"Unknown ID: $id")))(form => clockAction(form, fb))
  }

  private def clockAction(form: Form[ClockPlayback], feedback: Option[String] = None) =
    PimpAction(Ok(html.alarmEditor(form, feedback)(messages)))

  def newClock() = formSubmission(clockForm)(
    err => {
      html.alarmEditor(err)(messages)
    },
    (req, form, ap) => {
      ScheduledPlaybackService.save(ap)
      log.info(s"User: ${req.user} from: ${req.remoteAddress} saved alarm: $ap")
      Ok(html.alarmEditor(form, Some("Saved."))(messages))
    })

  private def formSubmission[T, C](form: Form[T])(err: Form[T] => C, ok: (AuthRequest[AnyContent], Form[T], T) => Result)(implicit w: Writeable[C]) =
    PimpAction(implicit request => handle(form)(err, (form, ap) => ok(request, form, ap)))

  private def handle[T, C](form: Form[T])(errorContent: Form[T] => C, okRedir: (Form[T], T) => Result)(implicit request: Request[_], w: Writeable[C]) = {
    val filledForm = form.bindFromRequest()
    filledForm.fold(errors => {
      BadRequest(errorContent(errors))
    }, success => {
      okRedir(filledForm, success)
    })
  }
}

object AlarmEditor {
  private val log = Logger(getClass)
}
