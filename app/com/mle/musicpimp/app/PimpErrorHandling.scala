package com.mle.musicpimp.app

import com.mle.musicpimp.json.JsonMessages
import controllers.PimpContentController
import play.api.http.HttpErrorHandler
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.Future

/**
 * @author mle
 */
trait PimpErrorHandling extends HttpErrorHandler {
  abstract override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] =
    PimpContentController.pimpResult(
      html = super.onServerError(request, ex),
      json = Results.InternalServerError(JsonMessages.failure(s"${ex.getClass.getName}: ${ex.getMessage}")))(request)
}
