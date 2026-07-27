package controllers

import com.malliina.musicpimp.models.Reason
import com.malliina.play.http.CookiedRequest
import com.malliina.values.Username
import play.api.mvc.AnyContent

import scala.concurrent.Future

package object musicpimp:
  type PimpUserRequest = CookiedRequest[AnyContent, Username]

  def accessDenied = Reason.accessDenied

  def badRequest(message: String) = Reason.badRequest(message)

  def notFound(message: String) = Reason.notFound(message)

  def serverErrorGeneric = Reason.internalGeneric

  def serverError(message: String) = Reason.internal(message)

  def fut[T](body: T) = Future.successful(body)
