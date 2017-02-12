package controllers.musicpimp

import akka.stream.Materializer
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.TrackID
import com.malliina.musicpimp.scheduler._
import com.malliina.musicpimp.scheduler.web.SchedulerStrings
import com.malliina.musicpimp.tags.PimpTags
import com.malliina.play.Authenticator
import controllers.musicpimp.AlarmEditor.log
import play.api.Logger
import play.api.data.Forms._
import play.api.data.{Form, Forms}
import play.api.http.Writeable
import play.api.i18n.Messages
import play.api.mvc.Result

class AlarmEditor(tags: PimpTags, auth: Authenticator, messages: Messages, mat: Materializer)
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
    val (days, enabled, job) = parseDaysEnabledAndJob(ds, enabledOpt, TrackID(trackID))
    val s = ClockSchedule(hours, minutes, days)
    ClockPlayback(id, job, s, enabled)
  })(ap => {
    // decomposes a case class to an optional tuple of its constituent values
    val i = ap.when
    val trackID = ap.job.track
    val trackTitle = Library.findMeta(trackID).fold(trackID.id)(_.title)
    Some((ap.id, i.hour, i.minute, i.days.map(_.shortName), trackTitle, trackID.id, Some(if (ap.enabled) ON else OFF)))
  }))

  private def parseDaysEnabledAndJob(days: Seq[String], enabledOpt: Option[String], track: TrackID): (Seq[WeekDay], Boolean, PlaybackJob) = {
    val weekDays = days.flatMap(WeekDay.withShortName)
    val enabled = enabledOpt.contains(SchedulerStrings.ON)
    val job = PlaybackJob(track)
    (weekDays, enabled, job)
  }

  def newAlarm = clockAction(clockForm)

  def editAlarm(id: String, fb: Option[String] = None) = {
    ScheduledPlaybackService.find(id) map { clock =>
      val form = clockForm.fill(clock)
      clockAction(form, UserFeedback.formed(form))
    } getOrElse {
      pimpAction(notFound(s"Unknown ID: $id"))
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
      log.info(s"User: ${req.user} from: ${req.remoteAddress} saved alarm: $ap")
      Ok(tags.alarmEditor(form, Option(UserFeedback.success("Saved.")), req.user, messages))
    })

  private def formSubmission[T, C: Writeable](form: Form[T])(err: (PimpRequest, Form[T]) => C, ok: (PimpRequest, Form[T], T) => Result) =
    pimpAction(request => handle(form, request)(err, (form, ap) => ok(request, form, ap)))

  private def handle[T, C: Writeable](form: Form[T], request: PimpRequest)(errorContent: (PimpRequest, Form[T]) => C, okRedir: (Form[T], T) => Result) = {
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
