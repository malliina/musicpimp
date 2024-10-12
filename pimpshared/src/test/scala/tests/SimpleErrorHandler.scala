package tests

import play.api.http.HttpErrorHandler
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.Future

object SimpleErrorHandler extends HttpErrorHandler:
  override def onServerError(request: RequestHeader, exception: Throwable): Future[Result] =
    Future.successful(Results.InternalServerError)

  override def onClientError(
    request: RequestHeader,
    statusCode: Int,
    message: String
  ): Future[Result] =
    Future.successful(Results.BadRequest)
