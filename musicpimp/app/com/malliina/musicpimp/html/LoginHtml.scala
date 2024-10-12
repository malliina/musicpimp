package com.malliina.musicpimp.html

import com.malliina.musicpimp.html.PimpHtml.{feedbackDiv, postableForm, textInputBase}
import controllers.musicpimp.routes

import scalatags.Text.all.*

object LoginHtml extends PimpBootstrap:

  import tags.*

  def loginContent(conf: LoginContent) =
    val accounts = conf.accounts
    divClass(s"$Container login-container")(
      rowColumn(s"${col.md.four} $FormSignin")(
        conf.topFeedback.fold(empty)(feedbackDiv)
      ),
      rowColumn(col.md.four)(
        postableForm(
          routes.Accounts.formAuthenticate,
          `class` := FormSignin,
          name := "loginForm"
        )(
          h2("Please sign in"),
          formGroup(
            textInputBase(
              Text,
              accounts.userFormKey,
              Option("Username"),
              `class` := FormControl,
              autofocus
            )
          ),
          formGroup(
            textInputBase(
              Password,
              accounts.passFormKey,
              Option("Password"),
              `class` := FormControl
            )
          ),
          divClass(Checkbox)(
            label(
              input(
                `type` := Checkbox,
                value := True,
                name := accounts.rememberMeKey,
                id := accounts.rememberMeKey
              ),
              " Remember me"
            )
          ),
          blockSubmitButton()("Sign in")
        )
      ),
      rowColumn(col.md.four)(
        conf.formFeedback.fold(empty): fb =>
          alertDiv(s"${alert.danger} $FormSignin", fb.message)
      ),
      rowColumn(s"${col.md.four} $FormSignin")(
        conf.motd.fold(empty)(message => p(message))
      )
    )
