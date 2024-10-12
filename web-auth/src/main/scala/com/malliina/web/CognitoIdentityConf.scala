package com.malliina.web

import com.malliina.http.FullUrl

case class CognitoIdentityConf(clientId: String, domain: FullUrl, scope: String) {
  def authUrlGoogle(state: String, redirUrl: FullUrl) = authUrl("Google", state, redirUrl)

  def authUrlFacebook(state: String, redirUrl: FullUrl) = authUrl("Facebook", state, redirUrl)

  def authUrlAmazon(state: String, redirUrl: FullUrl) = authUrl("LoginWithAmazon", state, redirUrl)

  def authUrl(identityProvider: String, state: String, redirUrl: FullUrl): FullUrl = {
    val queryParams = Map(
      "identity_provider" -> identityProvider,
      "redirect_uri" -> redirUrl.url,
      "response_type" -> "code",
      "client_id" -> clientId,
      "scope" -> scope,
      "state" -> state
    )
    val stringParams = stringify(queryParams)
    domain.append(s"/oauth2/authorize?$stringParams")
  }

  def logoutUrl(callback: FullUrl): FullUrl = {
    val params = Map(
      "client_id" -> clientId,
      "logout_uri" -> callback.url
    )
    domain.append(s"/logout?${stringify(params)}")
  }

  def tokensUrl: FullUrl = domain.append("/oauth2/token")

  def tokenParameters(code: String, redirUrl: FullUrl): Map[String, String] =
    Map(
      "grant_type" -> "authorization_code",
      "client_id" -> clientId,
      "code" -> code,
      "redirect_uri" -> redirUrl.url
    )

  def stringify(map: Map[String, String]): String =
    map.map { case (key, value) => s"$key=$value" }.mkString("&")
}
