package controllers.musicpimp

import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.TrackID
import com.malliina.musicpimp.scheduler._
import com.malliina.musicpimp.scheduler.web.SchedulerStrings
import com.malliina.musicpimp.tags.PimpHtml
import controllers.musicpimp.AlarmEditor.log
import play.api.Logger
import play.api.data.Forms._
import play.api.data.{Form, Forms}
import play.api.http.Writeable
import play.api.i18n.Messages
import play.api.mvc.Result

class AlarmEditor(tags: PimpHtml,
                  auth: AuthDeps,
                  messages: Messages)
  extends Secured(auth)
    with SchedulerStrings {

  private val clockForm: Form[ClockPlayback] = Form(mapping(
    Id -> optional(text),
    Hours -> number(min = 0, max = 24),
    Minutes -> number(min = 0, max = 59),
    Days -> Forms.seq(text).verifying("Must select at least one day", _.nonEmpty),
    TrackKey -> nonEmptyText,
    TrackId -> nonEmptyText,
    Enabled -> optional(text)
  )((id, hours, minutes, ds, _, trackID, enabledOpt) => {
    // converts submitted form data to a case class
    val (days, enabled, job) = parseDaysEnabledAndJob(ds, enabledOpt, TrackID(trackID))
    val s = ClockSchedule(hours, minutes, days)
    ClockPlayback(id, job, s, enabled)
  })(ap => {
    // decomposes a case class to an optional tuple of its constituent values
    val i = ap.when
    val trackID = ap.job.track
    val trackTitle = Library.findMeta(trackID).fold(trackID.id)(_.title)
    Some((ap.id, i.hour, i.minute, i.days.map(_.shortName), trackTitle, trackID.id, Some(if (ap.enabled) On else OFF)))
  }))

  private def parseDaysEnabledAndJob(days: Seq[String], enabledOpt: Option[String], track: TrackID): (Seq[WeekDay], Boolean, PlaybackJob) = {
    val weekDays = days.flatMap(WeekDay.withShortName)
    val enabled = enabledOpt.contains(SchedulerStrings.On)
    val job = PlaybackJob(track)
    (weekDays, enabled, job)
  }

  def newAlarm = clockAction(clockForm)

  def editAlarm(id: String, fb: Option[String] = None) = {
    ScheduledPlaybackService.find(id) map { clock =>
      val form = clockForm.fill(clock)
      clockAction(form, UserFeedback.formed(form))
    } getOrElse {
      pimpAction(notFound(s"Unknown ID '$id'."))
    }
  }

  private def clockAction(form: Form[ClockPlayback], feedback: Option[UserFeedback] = None) =
    pimpAction(req => Ok(tags.alarmEditor(form, feedback, req.user, messages)))

  def newClock() = formSubmission(clockForm)(
    (req, err) => {
      tags.alarmEditor(err, None, req.user, messages)
    },
    (req, form, ap) => {
      ScheduledPlaybackService.save(ap)
      log.info(s"User '${req.user}' from '${req.remoteAddress}' saved alarm '$ap'.")
      Ok(tags.alarmEditor(form, Option(UserFeedback.success("Saved.")), req.user, messages))
    })

  private def formSubmission[T, C: Writeable](form: Form[T])(err: (PimpUserRequest, Form[T]) => C, ok: (PimpUserRequest, Form[T], T) => Result) =
    pimpAction(request => handle(form, request)(err, (form, ap) => ok(request, form, ap)))

  private def handle[T, C: Writeable](form: Form[T], request: PimpUserRequest)(errorContent: (PimpUserRequest, Form[T]) => C, okRedir: (Form[T], T) => Result) = {
    val filledForm = form.bindFromRequest()(request)
    filledForm.fold(
      errors => BadRequest(errorContent(request, errors)),
      success => okRedir(filledForm, success)
    )
  }
}

object AlarmEditor {
  private val log = Logger(getClass)
}
