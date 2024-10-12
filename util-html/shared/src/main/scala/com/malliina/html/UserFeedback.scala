package com.malliina.html

import com.malliina.html.UserFeedback.{Feedback, No, Success, Yes}

case class UserFeedback(message: String, isError: Boolean) {
  def toMap: Seq[(String, String)] = Seq(
    Feedback -> message,
    Success -> (if (isError) No else Yes)
  )
}

object UserFeedback {
  val Feedback = "feedback"
  val Success = "success"
  val Yes = "yes"
  val No = "no"

  def success(message: String) = UserFeedback(message, isError = false)

  def error(message: String) = UserFeedback(message, isError = true)
}
