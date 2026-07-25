package com.malliina.musicpimp.http4s

import cats.Applicative
import cats.effect.Concurrent
import com.malliina.http.Errors
import com.malliina.http4s.FeedbackSupport
import com.malliina.musicpimp.html.UriSyntax
import com.malliina.musicpimp.json.MediaRanges
import com.malliina.play.tags.TagPage
import com.malliina.util.AppLogger
import io.circe.Encoder
import org.http4s.*
import org.http4s.headers.`Content-Type`
import scalatags.generic.Frag

trait MyScalatagsInstances:
  given tagPageEncode[F[_]]: EntityEncoder[F, TagPage] = scalatagsEncoder.contramap[TagPage](_.tags)
  given scalatagsEncoder[F[_], C <: Frag[?, String]](using
    charset: Charset = Charset.`UTF-8`
  ): EntityEncoder[F, C] =
    contentEncoder(MediaType.text.html)

  private def contentEncoder[F[_], C <: Frag[?, String]](mediaType: MediaType)(using
    charset: Charset
  ): EntityEncoder[F, C] =
    EntityEncoder
      .stringEncoder[F]
      .contramap[C](content => content.render)
      .withContentType(`Content-Type`(mediaType, charset))

trait AppParsers[F[_]: Applicative] extends Responses[F]:
  protected def parsed[T](result: Either[Errors, T])(
    res: T => F[Response[F]]
  ): F[Response[F]] = result.fold(
    errors => badRequestWithErrors(errors),
    ok => res(ok)
  )

trait AppImplicits[F[_]: Concurrent]
  extends AppParsers[F]
  with MyScalatagsInstances
  with PimpDecoders[F]
  with FeedbackSupport[F]
  with UriSyntax:
  private val log = AppLogger(getClass)

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
