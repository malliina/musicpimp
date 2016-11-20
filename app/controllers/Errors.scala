package controllers

import com.malliina.musicpimp.json.JsonMessages
import play.api.mvc.Results
import play.api.mvc.Results.{BadRequest, InternalServerError, NotFound, Unauthorized}

object Errors extends Errors

trait Errors {
  val genericMessage = "Something went wrong."
  val accessDeniedMessage = "Access denied."

  def accessDenied = withStatus(Unauthorized, accessDeniedMessage)

  def badRequest(message: String) = withStatus(BadRequest, message)

  def notFound(message: String) = withStatus(NotFound, message)

  def internalGeneric = internal(genericMessage)

  def internal(message: String) = withStatus(InternalServerError, message)

  def withStatus(status: Results.Status, message: String) = status(JsonMessages.failure(message))
}
