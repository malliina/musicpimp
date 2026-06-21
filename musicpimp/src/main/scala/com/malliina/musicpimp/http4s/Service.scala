package com.malliina.musicpimp.http4s

import cats.effect.Async
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.malliina.musicpimp.BuildInfo
import com.malliina.musicpimp.auth.{AuthBundle, AuthedRequest}
import com.malliina.musicpimp.html.PimpHtml
import com.malliina.musicpimp.library.{MusicFolder, MusicLibrary}
import io.circe.{Encoder, Json}
import io.circe.syntax.EncoderOps
import org.http4s.HttpRoutes

class Service[F[_]: Async](
  webAuth: AuthBundle[F, AuthedRequest[F]],
  lib: MusicLibrary[F],
  html: PimpHtml
) extends AppImplicits[F]:
  val routes = HttpRoutes.of[F]:
    case req @ GET -> Root =>
      ok(Json.obj("git" -> BuildInfo.gitHash.asJson))
    case req @ GET -> Root / "folders" =>
      webAuth.authenticator
        .authenticate(req)
        .flatMap: outcome =>
          outcome.fold(
            failure => webAuth.onUnauthorized(failure),
            user =>
              lib.rootFolder.flatMap: root =>
                given Encoder[MusicFolder] = MusicFolder.writer(req)
                pimpResult(req)(
                  html = ok(html.flexLibrary(root, user.user.username)),
                  json = ok(root)
                )
          )
