package com.malliina.oauth

import com.malliina.http.FullUrl
import com.malliina.values.Email

import scala.concurrent.{ExecutionContext, Future}

trait GoogleOAuthLike extends AutoCloseable {
  def ec: ExecutionContext

  def discover(): Future[GoogleOAuthConf]

  def authRequestUri(authEndpoint: FullUrl, redirectUri: FullUrl, state: String): FullUrl

  def resolveEmail(tokenEndpoint: FullUrl, code: String, redirectUri: FullUrl): Future[Email]
}
