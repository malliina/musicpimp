package controllers

import com.mle.musicpimp.auth.Token
import com.mle.util.Log

/**
 * @author Michael
 */
trait TokenLogging extends TokenStore with Log {
  abstract override def persist(token: Token): Unit = {
    super.persist(token)
    log debug s"Persisted token: $token"
  }

  abstract override def remove(token: Token): Unit = {
    super.remove(token)
    log debug s"Removed token: $token"
  }
}
