package com.malliina.pimpcloud.http4s

import com.malliina.http.SingleError

enum AuthProvider(val name: String):
  case Google extends AuthProvider("google")

object AuthProvider:
  val PromptKey = "prompt"
  val SelectAccount = "select_account"

  private def forString(s: String): Either[SingleError, AuthProvider] =
    Seq(Google)
      .find(_.name == s)
      .toRight(SingleError(s"Unknown auth provider: '$s'."))

  def unapply(str: String): Option[AuthProvider] =
    forString(str).toOption
