package com.malliina.musicpimp.auth

import com.malliina.play.auth._
import com.malliina.play.http.AuthedRequest
import com.malliina.values.{Password, Username}
import controllers.musicpimp.fut
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

object Auths {
  private val log = Logger(getClass)

  val session = Authenticator[AuthedRequest] { rh =>
    val result = rh.session.get(com.malliina.play.auth.Auth.DefaultSessionKey).map(Username.apply)
      .map(user => Right(new AuthedRequest(user, rh)))
      .getOrElse(Left(InvalidCredentials(rh)))
    fut(result)
  }

  def anyOne[T](auths: Authenticator[T]*)(implicit ec: ExecutionContext): Authenticator[T] =
    Authenticator[T] { rh =>
      first(auths.toList)(_.authenticate(rh))(_.isRight)
    }

  /** Sequentially evaluates `f` on elements in `ts` until `p` evaluates to true.
    *
    * If `p` does not evaluate to true to any element in `ts`, the result of `f`
    * on the last element in `ts` is returned. If `ts` is empty a failed `Future`
    * is returned.
    *
    * @return the first result that satisfies `p`, or the last result if there's no match
    */
  def first[T, R](ts: List[T])(f: T => Future[R])(p: R => Boolean)(implicit ec: ExecutionContext): Future[R] =
    ts match {
      case Nil =>
        Future.failed(new NoSuchElementException)
      case head :: tail =>
        f(head) flatMap { res =>
          if (p(res) || tail.isEmpty) fut(res)
          else first(tail)(f)(p)
        }
    }
}

class Auths(userManager: UserManager[Username, Password], rememberMe: RememberMe)(implicit ec: ExecutionContext) {
  private val database = Authenticator[AuthedRequest] { rh =>
    com.malliina.play.auth.Auth.basicCredentials(rh) map { creds =>
      userManager.authenticate(creds.username, creds.password) map { isValid =>
        if (isValid) Right(AuthedRequest(creds.username, rh, None))
        else Left(InvalidCredentials(rh))
      }
    } getOrElse {
      fut(Left(MissingCredentials(rh)))
    }
  }

  val cookie = Authenticator[AuthedRequest] { rh =>
    rememberMe.authenticateFromCookie(rh).map(_.toRight(MissingCredentials(rh)))
  }

  val client = Auths.anyOne(Auths.session, cookie, database)
}
