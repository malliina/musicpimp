package com.malliina.play.forms

import com.malliina.values._
import play.api.data.format.Formats.{longFormat, stringFormat}
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.{Forms, Mapping}

object FormMappings extends FormMappings

trait FormMappings {
  val username: Mapping[Username] = stringMapping[Username](Username.build)
  val password: Mapping[Password] = stringMapping[Password](Password.build)
  val email: Mapping[Email] = stringMapping[Email](Email.build)
  val accessToken: Mapping[AccessToken] = stringMapping[AccessToken](AccessToken.build)
  val idToken: Mapping[IdToken] = stringMapping[IdToken](IdToken.build)
  val userId: Mapping[UserId] = Forms.of[Long].transform(l => UserId(l), u => u.id)

  val c: Constraint[String] =
    Constraint[String]((s: String) => Email.build(s).fold(err => Invalid(err.message), _ => Valid))

  def stringMapping[T <: WrappedString](build: String => Either[ErrorMessage, T]): Mapping[T] =
    Forms
      .of[String]
      .verifying(
        Constraint[String]((s: String) => build(s).fold(err => Invalid(err.message), _ => Valid))
      )
      .transform(
        s => build(s).getOrElse(throw new NoSuchElementException(s"Invalid input: '$s")),
        t => t.value
      )
}
