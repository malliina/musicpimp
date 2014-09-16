package com.mle.musicpimp.auth

import com.mle.musicpimp.util.FileUtil
import com.mle.play.auth.RememberMe

/**
 * @author Michael
 */
object CookieLogin extends RememberMe(new TokensStore(FileUtil.pathTo("tokens.json", createIfNotExists = true)))