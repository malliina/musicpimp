package controllers

import play.api.data.{Forms, Form}
import play.api.data.Forms._
import play.api.mvc.Request
import play.api.http.Writeable
import com.mle.musicpimp.scheduler.web.SchedulerStrings
import com.mle.musicpimp.scheduler._
import scala.Some
import play.api.mvc.SimpleResult

/**
 *
 * @author mle
 */
trait AlarmEditor extends Secured with SchedulerStrings {
  private val clockForm: Form[ClockPlayback] = Form(mapping(
    ID -> optional(text),
    HOURS -> number(min = 0, max = 24),
    MINUTES -> number(min = 0, max = 59),
    DAYS -> Forms.seq(text).verifying("Must select at least one day", _.size > 0),
    TRACK -> nonEmptyText,
    ENABLED -> optional(text)
  )((id, hours, minutes, ds, track, enabledOpt) => {
    // converts submitted form data to a case class
    val (days, enabled, job) = parseDaysEnabledAndJob(ds, enabledOpt, track)
    val s = ClockSchedule(hours, minutes, days)
    ClockPlayback(id, job, s, enabled)
  })(ap => {
    // decomposes a case class to an optional tuple of its constituent values
    val i = ap.when
    Some((ap.id, i.hour, i.minute, i.days.map(_.shortName), ap.job.track, Some(if (ap.enabled) ON else OFF)))
  }))

  private def parseDaysEnabledAndJob(days: Seq[String], enabledOpt: Option[String], track: String): (Seq[WeekDay], Boolean, PlaybackJob) = {
    val weekDays = days.flatMap(WeekDay.withShortName)
    val enabled = enabledOpt.exists(_ == SchedulerStrings.ON)
    val job = PlaybackJob(track)
    (weekDays, enabled, job)
  }

  def newAlarm = clockAction(clockForm)

  def editAlarm(id: String, fb: Option[String] = None) = {
    APManager.find(id).map(clockForm.fill)
      .map(clockAction(_, fb))
      .getOrElse(PimpAction(NotFound(s"Unknown ID: $id")))
  }

  private def clockAction(form: Form[ClockPlayback], feedback: Option[String] = None) =
    PimpAction(Ok(views.html.alarmEditor(form, feedback)))

  def newClock() = formSubmission(clockForm)(
    err => views.html.alarmEditor(err),
    (form, ap) => {
      APManager.save(ap)
      Ok(views.html.alarmEditor(form, Some("Saved.")))
    })

  private def formSubmission[T, C](form: Form[T])(err: Form[T] => C, ok: (Form[T], T) => SimpleResult)(implicit w: Writeable[C]) =
    PimpAction(implicit request => handle(form)(err, ok))

  private def handle[T, C](form: Form[T])(errorContent: Form[T] => C, okRedir: (Form[T], T) => SimpleResult)(implicit request: Request[_], w: Writeable[C]) = {
    val filledForm = form.bindFromRequest()
    filledForm.fold(errors => {
      BadRequest(errorContent(errors))
    }, success => {
      okRedir(filledForm, success)
    })
  }
}
