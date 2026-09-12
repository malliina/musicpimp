package com.malliina.http4s

import cats.data.NonEmptyList
import cats.effect.{Async, Sync}
import cats.syntax.all.{catsSyntaxApplicativeId, toFlatMapOps}
import com.malliina.http4s.StaticService.log
import com.malliina.util.AppLogger
import com.malliina.values.UnixPath
import fs2.io.file.{Files, Path}
import org.http4s.CacheDirective.*
import org.http4s.headers.`Cache-Control`
import org.http4s.{Header, HttpRoutes, Request, StaticFile}
import org.typelevel.ci.CIStringSyntax
import java.nio.file.Path as JPath
import scala.concurrent.duration.DurationInt

object StaticService:
  private val log = AppLogger(getClass)

  def paths[F[_]: {Async, Files}](
    assetsDir: JPath,
    assetsPrefix: String,
    isProd: Boolean
  ): StaticService[F] =
    StaticService(
      Path.fromNioPath(assetsDir).absolute,
      assetsPrefix,
      isProd
    )

class StaticService[F[_]: {Async, Files}](
  assetsDir: Path,
  assetsPrefix: String,
  isProd: Boolean
) extends BasicApiService[F]:
  private val fontExtensions = List(".woff", ".woff2", ".eot", ".ttf")
  private val supportedStaticExtensions =
    List(".html", ".js", ".map", ".css", ".png", ".ico", ".svg", ".map", ".json") ++ fontExtensions

  private val allowAllOrigins = Header.Raw(ci"Access-Control-Allow-Origin", "*")

  val routes: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ GET -> rest if supportedStaticExtensions.exists(rest.toString.endsWith) =>
      val file = UnixPath(rest.segments.mkString("/"))
      val isCacheable =
        (file.value.count(_ == '.') == 2 || file.value.startsWith("static/")) &&
          !file.value.endsWith(".map")
      val cacheHeaders =
        if isCacheable then NonEmptyList.of(`max-age`(365.days), `public`)
        else BasicApiService.noCacheDirectives

      val search =
        if isProd then
          val resourcePath = s"$assetsPrefix${file.value}"
          log.debug(s"Searching for resource '$resourcePath'...")
          StaticFile.fromResource(resourcePath, Option(req))
        else
          val assetPath: fs2.io.file.Path = assetsDir.resolve(file.value)
          log.debug(s"Searching for file '${assetPath.toNioPath.toAbsolutePath}'...")
          StaticFile.fromPath(assetPath, Option(req))
      search
        .map(_.putHeaders(`Cache-Control`(cacheHeaders), allowAllOrigins))
        .fold(onNotFound(req))(_.pure[F])
        .flatten

  private def onNotFound(req: Request[F]) =
    Sync[F]
      .delay(log.info(s"Not found '${req.uri}'."))
      .flatMap: _ =>
        notFound(s"Not found '${req.uri}'.")
