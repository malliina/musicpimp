package com.malliina.musicpimp.http4s

import cats.data.NonEmptyList
import cats.effect.{Async, Sync}
import cats.syntax.all.{catsSyntaxApplicativeId, toFlatMapOps}
import com.malliina.musicpimp.BuildInfo
import com.malliina.musicpimp.http4s.StaticService.log
import com.malliina.util.AppLogger
import com.malliina.values.UnixPath
import fs2.io.file.Files
import org.http4s.CacheDirective.*
import org.http4s.headers.`Cache-Control`
import org.http4s.{Header, HttpRoutes, Request, StaticFile}
import org.typelevel.ci.CIStringSyntax

import scala.concurrent.duration.DurationInt

object StaticService:
  private val log = AppLogger(getClass)

class StaticService[F[_]: { Async, Files }] extends BasicApiService[F]:
  private val fontExtensions = List(".woff", ".woff2", ".eot", ".ttf")
  private val supportedStaticExtensions =
    List(".html", ".js", ".map", ".css", ".png", ".ico", ".svg", ".map", ".json") ++ fontExtensions

  private val assetsDir = fs2.io.file.Path(BuildInfo.assetsDir.getAbsolutePath)
  private val publicDir = fs2.io.file.Path(BuildInfo.publicDir)
  private val allowAllOrigins = Header.Raw(ci"Access-Control-Allow-Origin", "*")

  val routes: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ GET -> rest if supportedStaticExtensions.exists(rest.toString.endsWith) =>
      val file = UnixPath(rest.segments.mkString("/"))
      val isCacheable =
        (file.value.count(_ == '.') == 2 || file.value.startsWith("static/")) &&
          !file.value.endsWith(".map")
      val cacheHeaders =
        if isCacheable then NonEmptyList.of(`max-age`(365.days), `public`)
        else NonEmptyList.of(`no-cache`(), `no-store`, `must-revalidate`)

      val search =
        if BuildInfo.isProd then
          val resourcePath = s"${BuildInfo.publicFolder}/${file.value}"
          log.debug(s"Searching for resource '$resourcePath'...")
          StaticFile.fromResource(resourcePath, Option(req))
        else
          val assetPath: fs2.io.file.Path = assetsDir.resolve(file.value)
          val publicPath = publicDir.resolve(file.value)
          log.debug(
            s"Searching for file '${assetPath.toNioPath.toAbsolutePath}' or '${publicPath.toNioPath.toAbsolutePath}'..."
          )
          StaticFile
            .fromPath(assetPath, Option(req))
            .orElse(StaticFile.fromPath(publicPath, Option(req)))

      search
        .map(_.putHeaders(`Cache-Control`(cacheHeaders), allowAllOrigins))
        .fold(onNotFound(req))(_.pure[F])
        .flatten

  private def onNotFound(req: Request[F]) =
    Sync[F]
      .delay(log.info(s"Not found '${req.uri}'."))
      .flatMap: _ =>
        notFound(s"Not found '${req.uri}'.")
