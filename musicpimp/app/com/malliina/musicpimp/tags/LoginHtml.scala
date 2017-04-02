package com.malliina.musicpimp.tags

import com.malliina.musicpimp.tags.PimpHtml.{True, feedbackDiv, postableForm, textInputBase}
import com.malliina.play.controllers.AccountForms
import com.malliina.play.tags.All._
import controllers.musicpimp.{UserFeedback, routes}

import scalatags.Text.all._

object LoginHtml {
  def loginContent(accounts: AccountForms,
                   motd: Option[String],
                   formFeedback: Option[UserFeedback],
                   topFeedback: Option[UserFeedback]) = divContainer(
    rowColumn(s"$ColMd4 $FormSignin")(
      topFeedback.fold(empty)(feedbackDiv)
    ),
    rowColumn(ColMd4)(
      postableForm(routes.Accounts.formAuthenticate(), `class` := FormSignin, name := "loginForm")(
        h2("Please sign in"),
        formGroup(
          textInputBase(Text, accounts.userFormKey, Option("Username"), `class` := FormControl, autofocus)
        ),
        formGroup(
          textInputBase(Password, accounts.passFormKey, Option("Password"), `class` := FormControl)
        ),
        divClass(Checkbox)(
          label(
            input(`type` := Checkbox, value := True, name := accounts.rememberMeKey, id := accounts.rememberMeKey),
            " Remember me"
          )
        ),
        blockSubmitButton()("Sign in")
      )
    ),
    rowColumn(ColMd4)(
      formFeedback.fold(empty) { fb =>
        alertDiv(s"$AlertDanger $FormSignin", fb.message)
      }
    ),
    rowColumn(s"$ColMd4 $FormSignin")(
      motd.fold(empty)(message => p(message))
    )
  )
}