package controllers

import com.malliina.musicpimp.models.Errors
import com.malliina.play.http.CookiedRequest
import com.malliina.play.models.Username
import play.api.mvc.AnyContent

import scala.concurrent.Future

package object musicpimp {
  type PimpUserRequest = CookiedRequest[AnyContent, Username]

  def accessDenied = Errors.accessDenied

  def badRequest(message: String) = Errors.badRequest(message)

  def notFound(message: String) = Errors.notFound(message)

  def serverErrorGeneric = Errors.internalGeneric

  def serverError(message: String) = Errors.internal(message)

  def fut[T](body: T) = Future.successful(body)
}
