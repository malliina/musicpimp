package com.mle.musicpimp.auth

import com.mle.file.FileUtilities
import com.mle.play.auth.RememberMe

/**
 * @author Michael
 */
object CookieLogin extends RememberMe(new TokensStore(FileUtilities.pathTo("tokens.json")))