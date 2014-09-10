package controllers

import com.mle.musicpimp.auth.TokensStore

/**
 * @author Michael
 */
object CookieLogin extends RememberMe {
  override val store: TokenStore = new TokensStore with TokenLogging
}
