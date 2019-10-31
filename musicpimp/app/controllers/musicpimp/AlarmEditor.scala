package controllers.musicpimp

import com.malliina.musicpimp.html.{AlarmContent, PimpHtml}
import com.malliina.musicpimp.models.TrackID
import com.malliina.musicpimp.scheduler._
import com.malliina.musicpimp.scheduler.web.SchedulerStrings
import controllers.musicpimp.AlarmEditor.log
import play.api.Logger
import play.api.data.Forms._
import play.api.data.{Form, Forms}
import play.api.http.Writeable
import play.api.i18n.Messages
import play.api.mvc.Result

class AlarmEditor(
  val schedules: ScheduledPlaybackService,
  tags: PimpHtml,
  auth: AuthDeps,
  messages: Messages
) extends Secured(auth)
  with SchedulerStrings {

  private val clockForm: Form[ClockPlaybackConf] = Form(
    mapping(
      Id -> optional(text),
      Hours -> number(min = 0, max = 24),
      Minutes -> number(min = 0, max = 59),
      Days -> Forms.seq(text).verifying("Must select at least one day", _.nonEmpty),
      TrackKey -> nonEmptyText,
      TrackId -> nonEmptyText,
      Enabled -> optional(text)
    )((id, hours, minutes, ds, _, trackID, enabledOpt) => {
      // converts submitted form data to a case class
      val (days, enabled) = parseDaysEnabledAndJob(ds, enabledOpt)
      val s = ClockSchedule(hours, minutes, days)
      ClockPlaybackConf(id, TrackID(trackID), s, enabled)
    })(ap => {
      // decomposes a case class to an optional tuple of its constituent values
      val i = ap.when
      Some(
        (
          ap.id,
          i.hour,
          i.minute,
          i.days.map(_.shortName),
          ap.track.id,
          ap.track.id,
          Some(
            if (ap.enabled) On
            else OFF
          )
        )
      )
    })
  )

  private def parseDaysEnabledAndJob(
    days: Seq[String],
    enabledOpt: Option[String]
  ): (Seq[WeekDay], Boolean) = {
    val weekDays = days.flatMap(WeekDay.withShortName)
    val enabled = enabledOpt.contains(SchedulerStrings.On)
    (weekDays, enabled)
  }

  def newAlarm = clockAction(clockForm)

  def editAlarm(id: String, fb: Option[String] = None) = {
    schedules.find(id) map { clock =>
      val form = clockForm.fill(clock)
      clockAction(form, UserFeedback.formed(form))
    } getOrElse {
      pimpAction(notFound(s"Unknown ID '$id'."))
    }
  }

  private def clockAction(form: Form[ClockPlaybackConf], feedback: Option[UserFeedback] = None) =
    pimpAction(req => Ok(tags.alarmEditor(AlarmContent(form, feedback, req.user, messages))))

  def newClock() = formSubmission(clockForm)(
    (req, err) => {
      tags.alarmEditor(AlarmContent(err, None, req.user, messages))
    },
    (req, form, ap) => {
      schedules.save(ap)
      log.info(s"User '${req.user}' from '${req.remoteAddress}' saved alarm '$ap'.")
      Ok(
        tags.alarmEditor(
          AlarmContent(form, Option(UserFeedback.success("Saved.")), req.user, messages)
        )
      )
    }
  )

  private def formSubmission[T, C: Writeable](
    form: Form[T]
  )(err: (PimpUserRequest, Form[T]) => C, ok: (PimpUserRequest, Form[T], T) => Result) =
    pimpAction(request => handle(form, request)(err, (form, ap) => ok(request, form, ap)))

  private def handle[T, C: Writeable](
    form: Form[T],
    request: PimpUserRequest
  )(errorContent: (PimpUserRequest, Form[T]) => C, okRedir: (Form[T], T) => Result) = {
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
