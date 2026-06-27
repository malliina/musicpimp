package com.malliina.musicpimp.http4s

import cats.effect.Concurrent
import com.malliina.http4s.{FormDecoders, FormReadableT}
import com.malliina.play.auth.{BasicCredentials, RememberMeCredentials}
import com.malliina.play.controllers.AccountKeys
import com.malliina.play.models.PasswordChange
import com.malliina.values.{Password, Username}

trait PimpDecoders[F[_]: Concurrent] extends FormDecoders[F]:
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
    yield PasswordChange(oldPass, newPass, newPassAgain)
