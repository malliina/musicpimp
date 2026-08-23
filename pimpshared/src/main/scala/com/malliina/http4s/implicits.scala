package com.malliina.http4s

import cats.Applicative
import cats.effect.Concurrent
import com.malliina.html.UserFeedback
import com.malliina.http.Errors
import com.malliina.musicpimp.html.TagPage
import com.malliina.musicpimp.http4s.Responses
import org.http4s.headers.`Content-Type`
import org.http4s.{Charset, EntityEncoder, MediaType, Request, Response}
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
  with FeedbackSupport[F]:

  extension (req: Request[?])
    def userFeedback(cookieName: String = feedbackCookieName): Option[UserFeedback] =
      req.cookies
        .find(_.name == cookieName)
        .map(_.content)
        .flatMap(f => io.circe.parser.decode[UserFeedback](f).toOption)
