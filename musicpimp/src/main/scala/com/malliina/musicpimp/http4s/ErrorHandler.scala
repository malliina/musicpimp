package com.malliina.musicpimp.http4s

import cats.effect.Async
import com.malliina.http.{HttpResponse, ResponseException}
import com.malliina.musicpimp.http4s.ErrorHandler.log
import com.malliina.util.AppLogger
import org.http4s.{Request, Response}

import scala.util.control.NonFatal

object ErrorHandler:
  private val log = AppLogger(getClass)

class ErrorHandler[F[_]: Async] extends BasicApiService[F] with AppImplicits[F]:
  def handler: Request[F] => PartialFunction[Throwable, F[Response[F]]] =
    _ => partial

  def partial: PartialFunction[Throwable, F[Response[F]]] =
    case re: ResponseException =>
      val error = re.error
      error.response match
        case res: HttpResponse =>
          log.error(s"HTTP ${error.code} for '${error.url}'. Body: '${res.asString}'.")
        case _ =>
          log.error(s"HTTP ${error.code} for '${error.url}'.")
      serverError
    case NonFatal(t) =>
      log.error(s"Server error.", t)
      serverError
