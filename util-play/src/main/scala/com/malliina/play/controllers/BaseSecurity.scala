package com.malliina.play.controllers

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.apache.pekko.stream.Materializer
import com.malliina.play.auth.*
import com.malliina.play.controllers.BaseSecurity.log
import com.malliina.play.http.*
import com.malliina.play.models.AuthInfo
import play.api.Logger
import play.api.libs.streams.Accumulator
import play.api.mvc.*

import scala.concurrent.{ExecutionContextExecutor, Future}

object BaseSecurity:
  private val log = Logger(getClass)

  def logged(action: EssentialAction): EssentialAction = EssentialAction: rh =>
    log debug s"Request '${rh.path}' from '${Proxies.realAddress(rh)}'."
    action(rh)

/** @param actions
  *   action builder
  * @param mat
  *   materilalizer
  * @param auth
  *   authenticator
  * @tparam A
  *   type of authenticated user
  */
class BaseSecurity[A <: AuthInfo](
  actions: ActionBuilder[Request, AnyContent],
  auth: AuthBundle[A],
  val mat: Materializer
):
  implicit val ec: ExecutionContextExecutor = mat.executionContext

  /** Called when an unauthorized request has been made. Also called when a failed authentication
    * attempt is made.
    *
    * @param failure
    *   header auth failure, including request headers
    * @return
    *   "auth failed" result
    */
  protected def onUnauthorized(failure: AuthFailure): Result =
    auth.onUnauthorized(failure)

  /** Retrieves the authenticated username from the request.
    *
    * Attempts to read the "username" session variable, but if no such thing exists, attempts to
    * authenticate based on the HTTP Authorization header, finally if that also fails, authenticates
    * based on credentials in the query string.
    *
    * @return
    *   the authentication result
    */
  def authenticate(rh: RequestHeader): Future[Either[AuthFailure, A]] =
    auth.authenticator.authenticate(rh)

  // TODO make an ActionBuilder of A instead, then remove dependency on ActionBuilder in this class

  def authActionAsync(f: A => Future[Result]) =
    authenticatedLogged((user: A) => actions.async(_ => f(user)))

  def authAction(f: A => Result) =
    authenticatedLogged((user: A) => actions(f(user)))

  def authenticatedLogged(f: A => EssentialAction): EssentialAction =
    authenticated((user: A) => logged(user, f))

  def authenticatedLogged(f: => EssentialAction): EssentialAction =
    authenticatedLogged((_: A) => f)

  def authenticated(f: => EssentialAction): EssentialAction =
    authenticated((_: A) => f)

  def authenticated(f: A => EssentialAction): EssentialAction =
    authenticatedAsync(req => authenticate(req), failure => onUnauthorized(failure))(f)

  /** Logs authenticated requests.
    */
  def logged(user: A, f: A => EssentialAction): EssentialAction =
    EssentialAction: rh =>
      logAuth(user, rh)
      f(user).apply(rh)

  def logAuth(user: A, rh: RequestHeader): Unit =
    log.info(s"User '${user.user}' from '${Proxies.realAddress(rh)}' requests '${rh.uri}'.")

  def logged(action: EssentialAction): EssentialAction =
    BaseSecurity.logged(action)

  /** Async version of Security.Authenticated.
    *
    * @param auth
    *   auth function
    * @param onUnauthorized
    *   callback if auth fails
    * @param action
    *   authenticated action
    * @return
    *   an authenticated action
    */
  def authenticatedAsync(
    auth: RequestHeader => Future[Either[AuthFailure, A]],
    onUnauthorized: AuthFailure => Result
  )(action: A => EssentialAction): EssentialAction =
    EssentialAction: rh =>
      val futureAccumulator = auth(rh).map: authResult =>
        authResult.fold(
          failure => Accumulator.done(onUnauthorized(failure)),
          success => action(success).apply(rh)
        )
      Accumulator.flatten(futureAccumulator)(mat)
