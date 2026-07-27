package com.malliina.musicpimp.http4s

import cats.effect.Concurrent
import ch.qos.logback.classic.Level
import com.malliina.http.Errors
import com.malliina.http4s.{FormDecoders, FormReadable, FormReadableT}
import com.malliina.musicpimp.html.ChangeLogLevel
import com.malliina.musicpimp.models.{NewUser, TrackID}
import com.malliina.musicpimp.scheduler.{ClockPlaybackConf, ClockSchedule, WeekDay}
import com.malliina.musicpimp.scheduler.web.{AlarmStrings, SchedulerStrings}
import com.malliina.play.auth.{BasicCredentials, RememberMeCredentials}
import com.malliina.play.controllers.AccountKeys
import com.malliina.play.models.PasswordChange
import com.malliina.values.{ErrorMessage, NonBlank, Password, Readable, Username}
import controllers.musicpimp.{Accounts, AlarmEditor, Cloud, RemoveToken}
import controllers.musicpimp.Cloud.ToggleCloudId
import org.http4s.UrlForm

trait FormReaders:
  given seq[T](using r: Readable[T]): FormReadable[Seq[T]] =
    (key: String, form: UrlForm) =>
      form
        .get(key)
        .toList
        .foldLeft[Either[ErrorMessage, Seq[T]]](Right(Nil)): (acc, s) =>
          r.read(s).fold(err => Left(err), ok => acc.map(oks => oks :+ ok))

  import AccountKeys.*

  private val reader = FormReadableT.reader

  given loginForm: FormReadableT[BasicCredentials] = reader.emap: form =>
    for
      user <- form.read[Username](userFormKey)
      pass <- form.read[Password](passFormKey)
    yield BasicCredentials(user, pass)

  given rememberMeForm: FormReadableT[RememberMeCredentials] = reader.emap: form =>
    for
      user <- form.read[Username](userFormKey)
      pass <- form.read[Password](passFormKey)
      remember <- form.read[Option[Boolean]](rememberMeKey)
    yield RememberMeCredentials(user, pass, remember.getOrElse(false))

  given changePassword: FormReadableT[PasswordChange] = reader.emap: form =>
    for
      oldPass <- form.read[Password](oldPassKey)
      newPass <- form.read[Password](newPassKey)
      newPassAgain <- form.read[Password](newPassAgainKey)
      _ <- Either.cond(
        newPass == newPassAgain,
        (),
        Errors.single("The new password was incorrectly repeated.")
      )
    yield PasswordChange(oldPass, newPass, newPassAgain)

  given newUserForm: FormReadableT[NewUser] = reader.emap: form =>
    for
      user <- form.read[Username](userFormKey)
      newPass <- form.read[Password](newPassKey)
      newPassAgain <- form.read[Password](newPassAgainKey)
      _ <- Either.cond(
        newPass == newPassAgain,
        (),
        Errors.single(Accounts.repeatPassFailureMessage)
      )
    yield NewUser(user, newPass, newPassAgain)

  given clockForm: FormReadableT[ClockPlaybackConf] = reader.emap: form =>
    for
      id <- form.read[Option[String]](AlarmStrings.Id)
      hours <- form
        .read[Int](SchedulerStrings.Hours)
        .flatMap: i =>
          if i >= 0 && i <= 24 then Right(i) else Left(Errors.single(s"Out of range: '$i'."))
      minutes <- form
        .read[Int](SchedulerStrings.Minutes)
        .flatMap: i =>
          if i >= 0 && i < 60 then Right(i) else Left(Errors.single(s"Out of range: '$i'."))
      days <- form
        .read[Seq[NonBlank]](s"${SchedulerStrings.Days}[]")
        .filterOrElse(_.nonEmpty, Errors.single("Must select at least one day."))
//      track <- form.read[NonBlank](TrackKey)
      trackId <- form.read[TrackID](SchedulerStrings.TrackId)
      enabledOpt <- form.read[Option[String]](SchedulerStrings.Enabled)
    yield
      val (ds, enabled) = AlarmEditor.parseDaysEnabledAndJob(days, enabledOpt)
      val s = ClockSchedule(hours, minutes, ds)
      ClockPlaybackConf(id, trackId, s, enabled)

  given cloudForm: FormReadableT[ToggleCloudId] = reader.emap: form =>
    for id <- form.read[Option[String]](Cloud.idFormKey)
    yield ToggleCloudId(id)

  given removalForm: FormReadableT[RemoveToken] = reader.emap: form =>
    for
      token <- form.read[String]("token")
      platform <- form.read[String]("platform")
    yield RemoveToken(token, platform)

  given changeLogLevelForm: FormReadableT[ChangeLogLevel] = reader.emap: form =>
    for level <- form.read[String](ChangeLogLevel.LevelKey)
    yield ChangeLogLevel(Level.toLevel(level))

trait PimpDecoders[F[_]: Concurrent] extends FormDecoders[F] with FormReaders
