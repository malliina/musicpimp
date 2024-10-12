package com.malliina.musicpimp.app

import com.malliina.musicpimp.http.PimpContentController
import com.malliina.musicpimp.models.Errors
import play.api.http.HttpErrorHandler
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.Future

trait PimpErrorHandling extends HttpErrorHandler:

  abstract override def onClientError(
    request: RequestHeader,
    statusCode: Int,
    message: String
  ): Future[Result] =
    PimpContentController.pimpResultUneven(request)(
      html = super.onClientError(request, statusCode, message),
      json = Errors.withStatus(Results.Status(statusCode), message)
    )

  abstract override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] =
    PimpContentController.pimpResultUneven(request)(
      html = super.onServerError(request, ex),
      json = Errors.internal(s"${ex.getClass.getName}: ${ex.getMessage}")
    )
