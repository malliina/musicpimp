package com.malliina.musicpimp.html

import com.malliina.musicpimp.html.PimpHtml.{True, feedbackDiv, postableForm, textInputBase}
import com.malliina.play.tags.All._
import controllers.musicpimp.routes

import scalatags.Text.all._

object LoginHtml {
  def loginContent(conf: LoginContent) = {
    val accounts = conf.accounts
    divClass(s"$Container login-container")(
      rowColumn(s"$ColMd4 $FormSignin")(
        conf.topFeedback.fold(empty)(feedbackDiv)
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
        conf.formFeedback.fold(empty) { fb =>
          alertDiv(s"$AlertDanger $FormSignin", fb.message)
        }
      ),
      rowColumn(s"$ColMd4 $FormSignin")(
        conf.motd.fold(empty)(message => p(message))
      )
    )
  }
}
