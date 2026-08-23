package com.malliina.pimpcloud.http4s

import cats.effect.Concurrent
import com.malliina.http4s.{AppImplicits, FormDecoders, FormReadableT}
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.http4s.AccountKeys.{passFormKey, userFormKey}
import com.malliina.values.{Password, Username}

object PimpExt extends PimpExt

trait PimpExt:
  extension [L, R](e: Either[L, R]) def handleLeft[S >: R](code: L => S): S = e.fold(code, identity)

trait CloudDecoders[F[_]: Concurrent] extends FormDecoders[F]:
  private val reader = FormReadableT.reader

  val serverFormKey = "server"

  given cloudForm: FormReadableT[CloudCreds] = reader.emap: form =>
    for
      cloudId <- form.read[CloudID](serverFormKey)
      user <- form.read[Username](userFormKey)
      pass <- form.read[Password](passFormKey)
    yield CloudCreds(cloudId, user, pass)

trait CloudImplicits[F[_]: Concurrent] extends AppImplicits[F] with CloudDecoders[F] with PimpExt
