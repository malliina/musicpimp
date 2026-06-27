package com.malliina.musicpimp.http4s

import cats.effect.{Async, Concurrent}
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.malliina.html.UserFeedback
import com.malliina.musicpimp.BuildInfo
import com.malliina.musicpimp.auth.{AuthBundle, AuthedRequest, Http4sAuth, PimpAuthenticator, Proxies2, UserManager, UserPayload}
import com.malliina.musicpimp.html.{LoginContent, PimpHtml}
import com.malliina.musicpimp.library.{MusicFolder, MusicLibrary}
import com.malliina.play.auth.RememberMeCredentials
import com.malliina.play.controllers.AccountKeys
import com.malliina.util.AppLogger
import com.malliina.values.{Password, Username}
import controllers.musicpimp.Accounts
import io.circe.{Encoder, Json}
import io.circe.syntax.EncoderOps
import org.http4s.HttpRoutes
import Service.log

object Service:
  private val log = AppLogger(getClass)

class Service[F[_]: { Async, Concurrent }](
  userManager: UserManager[F, Username, Password],
  auth: PimpAuthenticator[F],
  webAuth: AuthBundle[F, AuthedRequest[F]],
  cookies: Http4sAuth[F],
  lib: MusicLibrary[F],
  html: PimpHtml
) extends AppImplicits[F]:
  val accountKeys = AccountKeys
  val routes = HttpRoutes.of[F]:
    case req @ GET -> Root =>
      seeOther(Reverse.folders.base)
    case GET -> Root / "info" =>
      ok(Json.obj("git" -> BuildInfo.gitHash.asJson))
    case req @ GET -> Root / "login" =>
      userManager.isDefaultCredentials.flatMap: isDefault =>
        val motd =
          if isDefault then
            Option(
              Accounts.defaultCredentialsMessage(userManager.defaultUser, userManager.defaultPass)
            )
          else None
        ok(html.login(LoginContent(accountKeys, motd, None, req.feedbackAs[UserFeedback])))
    case req @ POST -> Root / "authenticate" =>
      req
        .attemptAs[RememberMeCredentials]
        .foldF(
          formErrors =>
            log.warn(s"Authentication failed: ${formErrors.message}")
            seeOther(Reverse.login).withFeedback(UserFeedback.error("Form error."))
          ,
          credentials =>
            val username = credentials.username
            auth
              .authenticate(username, credentials.password)
              .flatMap: isValid =>
                if isValid then
                  log.info(s"Authentication succeeded for user '$username'.")
                  val intendedUrl = cookies.readIntendedUri(req).getOrElse(reverse.folders.base)
                  seeOther(intendedUrl).map: res =>
                    cookies.withUser(UserPayload(username), Proxies2.isSecure(req), res)
                else
                  log.warn(s"Invalid form authentication for user '$username'.")
                  val formFeedback = UserFeedback.error("Incorrect username or password.")
                  val content = LoginContent(
                    accountKeys,
                    None,
                    Option(formFeedback),
                    req.feedbackAs[UserFeedback]
                  )
                  BadRequest(html.login(content))
        )
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
