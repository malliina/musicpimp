package com.malliina.play.controllers

import com.malliina.html.UserFeedback
import com.malliina.html.UserFeedback.{Feedback, No, Success}
import play.api.data.Form
import play.api.mvc.{Flash, RequestHeader}

object UserFeedbacks {
  def flashed(request: RequestHeader): Option[UserFeedback] =
    flashed(request.flash)

  def flashed(flash: Flash, textKey: String = Feedback): Option[UserFeedback] =
    for {
      message <- flash get textKey
      isError = (flash get Success) contains No
    } yield UserFeedback(message, isError)

  def formed(form: Form[?]): Option[UserFeedback] =
    form.globalError
      .orElse(form.errors.headOption)
      .map(formError => UserFeedback.error(formError.message))
}
