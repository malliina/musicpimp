package com.malliina.musicpimp.http4s

import cats.effect.Concurrent
import com.malliina.http4s.AppImplicits
import com.malliina.musicpimp.html.UriSyntax
import com.malliina.musicpimp.http4s.PimpImplicits.log
import com.malliina.musicpimp.json.MediaRanges
import com.malliina.musicpimp.html.TagPage
import com.malliina.util.AppLogger
import io.circe.Encoder
import org.http4s.*

object PimpImplicits:
  private val log = AppLogger(getClass)

trait PimpImplicits[F[_]: Concurrent] extends AppImplicits[F] with PimpDecoders[F] with UriSyntax:
  def respond[T: Encoder](req: Request[?])(html: => TagPage, json: => T) =
    pimpResult(req)(ok(html), ok(json))

  def response(
    req: Request[?]
  )(html: => F[Response[F]], json17: => F[Response[F]], latest: => F[Response[F]]): F[Response[F]] =
    requestedResponseFormat(req)
      .map:
        case MediaType.text.html => html
        case MediaRanges.JSONv17 => json17
        case MediaRanges.JSONv18 => latest
        case other =>
          val msg = s"Unknown response format: '$other'."
          log.warn(msg)
          notAcceptable(msg)
      .getOrElse:
        val msg =
          "No requested response format, unacceptable. Please provide a value in the 'Accept' header."
        log.warn(msg)
        notAcceptable(msg)
