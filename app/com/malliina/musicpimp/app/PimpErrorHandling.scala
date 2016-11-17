package com.malliina.musicpimp.app

import com.malliina.musicpimp.json.JsonMessages
import controllers.PimpContentController
import play.api.http.HttpErrorHandler
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.Future

trait PimpErrorHandling extends HttpErrorHandler {

  abstract override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] = {
    PimpContentController.pimpResult2(request)(
      html = super.onClientError(request, statusCode, message),
      json = Results.Status(statusCode).apply(JsonMessages.failure(message))
    )
  }

  abstract override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] =
    PimpContentController.pimpResult2(request)(
      html = super.onServerError(request, ex),
      json = Results.InternalServerError(JsonMessages.failure(s"${ex.getClass.getName}: ${ex.getMessage}"))
    )
}
