package com.malliina.web

object OAuthKeys extends OAuthKeys

trait OAuthKeys extends ClaimKeys {
  val AuthorizationCode = "authorization_code"
  val ClientIdKey = "client_id"
  val ClientSecretKey = "client_secret"
  val CodeKey = "code"
  val EmailKey = "email"
  val GrantType = "grant_type"
  val IdTokenKey = "id_token"
  val LoginHint = "login_hint"
  val Nonce = "nonce"
  val RedirectUri = "redirect_uri"
  val ResponseType = "response_type"
  val Scope = "scope"
  val State = "state"

  val scope = "openid email"
}

trait ClaimKeys {
  val Aud = "aud"
  val Exp = "exp"
  val IssuerKey = "iss"
  val Kid = "kid"
  val NotBefore = "nbf"
}
