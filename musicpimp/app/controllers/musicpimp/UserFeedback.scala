package controllers.musicpimp

import play.api.data.Form
import play.api.mvc.{Flash, RequestHeader}

case class UserFeedback(message: String, isError: Boolean)

object UserFeedback {
  val Feedback = "feedback"
  val Success = "success"
  val Yes = "yes"
  val No = "no"

  def success(message: String) = UserFeedback(message, isError = false)

  def error(message: String) = UserFeedback(message, isError = true)

  def flashed(request: RequestHeader): Option[UserFeedback] =
    flashed(request.flash)

  def flashed(flash: Flash, textKey: String = Feedback): Option[UserFeedback] =
    for {
      message <- flash get textKey
      isError = (flash get Success) contains No
    } yield UserFeedback(message, isError)

  def formed(form: Form[_]) =
    form.globalError.orElse(form.errors.headOption)
      .map(formError => error(formError.message))
}