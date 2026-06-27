package com.malliina.musicpimp.http4s

import cats.effect.{Async, Concurrent}
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.malliina.html.UserFeedback
import com.malliina.musicpimp.BuildInfo
import com.malliina.musicpimp.auth.{AuthBundle, AuthedRequest, Http4sAuth, PimpAuthenticator, Proxies2, UserManager, UserPayload}
import com.malliina.musicpimp.html.{LibraryContent, LoginContent, PimpHtml, UsersContent}
import com.malliina.musicpimp.library.{MusicFolder, MusicLibrary, Settings}
import com.malliina.play.auth.RememberMeCredentials
import com.malliina.play.controllers.AccountKeys
import com.malliina.util.AppLogger
import com.malliina.values.{Password, Username}
import controllers.musicpimp.{Accounts, SettingsController}
import io.circe.{Encoder, Json}
import io.circe.syntax.EncoderOps
import org.http4s.{HttpRoutes, Request, Response}
import Service.log
import com.malliina.musicpimp.audio.TrackJson
import com.malliina.musicpimp.stats.{DataRequest, PlaybackStats, PopularList, RecentList}
import com.malliina.play.http.FullUrls
import com.malliina.play.models.PasswordChange

object Service:
  private val log = AppLogger(getClass)

class Service[F[_]: { Async, Concurrent }](
  userManager: UserManager[F, Username, Password],
  auth: PimpAuthenticator[F],
  webAuth: AuthBundle[F, AuthedRequest[F]],
  cookies: Http4sAuth[F],
  lib: MusicLibrary[F],
  stats: PlaybackStats[F],
  html: PimpHtml
) extends AppImplicits[F]:
  private val accountKeys = AccountKeys
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
        ok(html.login(LoginContent(accountKeys, motd, None, req.userFeedback()))).clearFeedback
    case req @ GET -> Root / "logout" =>
      seeOther(reverse.login)
        .map(res => cookies.clearSession(res))
        .withFeedbackMessage(Accounts.logoutMessage)
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
                    req.userFeedback()
                  )
                  BadRequest(html.login(content)).clearFeedback
        )
    case req @ POST -> Root / "changePassword" =>
      authed(req): user =>
        req
          .attemptAs[PasswordChange]
          .foldF(
            formErrors =>
              log.warn(s"Authentication failed: ${formErrors.message}")
              badRequestEntity(html.account(user.username, req.userFeedback()))
            ,
            pc =>
              auth
                .authenticate(user.username, pc.oldPass)
                .flatMap: isValid =>
                  if isValid then
                    userManager
                      .updatePassword(user.username, pc.newPass)
                      .flatMap: _ =>
                        log.info(s"Password changed for user '$user'.")
                        seeOther(reverse.account)
                          .withFeedbackMessage(Accounts.passwordChangedMessage)
                  else
                    badRequestEntity(
                      html.account(
                        user.username,
                        Option(UserFeedback.error(Accounts.incorrectPasswordMessage))
                      )
                    )
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
    case req @ GET -> Root / "player" / "recent" =>
      metaAction(req): meta =>
        stats
          .mostRecent(meta)
          .flatMap: entries =>
            val list = RecentList.forEntries(meta, entries, FullUrls.hostOnly2(req))
            pimpResult(req)(
              html = ok(html.mostRecent(list)),
              json = ok(list)
            )
    case req @ GET -> Root / "player" / "popular" =>
      metaAction(req): meta =>
        stats
          .mostPlayed(meta)
          .flatMap: entries =>
            val list = PopularList.forEntries(meta, entries, FullUrls.hostOnly2(req))
            pimpResult(req)(
              html = ok(html.mostPopular(list)),
              json = ok(list)
            )
    case req @ GET -> Root / "settings" =>
      settings(req)
    case req @ GET -> Root / "manage" =>
      settings(req)
    case req @ GET -> Root / "about" =>
      authed(req): user =>
        ok(html.aboutBase(user.username))
    case req @ GET -> Root / "account" =>
      authed(req): user =>
        ok(html.account(user.username, req.userFeedback())).clearFeedback
    case req @ GET -> Root / "users" =>
      authed(req): user =>
        userManager.users.flatMap: users =>
          val feedback = req.userFeedback()
          val listFeedback = req.userFeedback(Accounts.UsersFeedback)
          val content = UsersContent(users, user.username, listFeedback, feedback)
          ok(html.users(content)).clearFeedback.map(_.removeCookie(Accounts.UsersFeedback))

  private def settings(req: Request[F]) =
    authed(req): user =>
      val fb = req.userFeedback()
      val content = LibraryContent(
        Settings.readFolders,
        SettingsController.folderPlaceHolder,
        user.username,
        fb
      )
      ok(html.musicFolders(content)).clearFeedback

  private def metaAction(req: Request[F])(code: DataRequest => F[Response[F]]) =
    authed(req): user =>
      DataRequest
        .fromReq(user.user.username, req)
        .fold(err => badRequestWithErrors(err), ok => code(ok))

  private def authed(req: Request[F])(code: AuthedRequest[F] => F[Response[F]]) =
    webAuth.authenticator
      .authenticate(req)
      .flatMap: outcome =>
        outcome.fold(failure => webAuth.onUnauthorized(failure), user => code(user))

  extension (req: Request[?])
    def userFeedback(cookieName: String = feedbackCookieName) =
      req.cookies
        .find(_.name == cookieName)
        .map(_.content)
        .flatMap(f => io.circe.parser.decode[UserFeedback](f).toOption)
