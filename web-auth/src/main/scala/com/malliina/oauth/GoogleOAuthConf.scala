package com.malliina.oauth

import com.malliina.http.FullUrl
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}

case class GoogleOAuthConf(
  issuer: String,
  authorizationEndpoint: FullUrl,
  tokenEndpoint: FullUrl,
  userInfoEndpoint: String,
  revocationEndpoint: String,
  jwksUri: String,
  responseTypesSupported: Seq[String],
  subjectTypesSupported: Seq[String],
  algorithmsSupported: Seq[String],
  scopesSupported: Seq[String],
  authMethodsSupported: Seq[String],
  claimsSupported: Seq[String]
)

object GoogleOAuthConf {
  import com.malliina.json.PlayFormats.url
  implicit val jsonReader: Reads[GoogleOAuthConf] = (
    (JsPath \ "issuer").read[String] and
      (JsPath \ "authorization_endpoint").read[FullUrl] and
      (JsPath \ "token_endpoint").read[FullUrl] and
      (JsPath \ "userinfo_endpoint").read[String] and
      (JsPath \ "revocation_endpoint").read[String] and
      (JsPath \ "jwks_uri").read[String] and
      (JsPath \ "response_types_supported").read[Seq[String]] and
      (JsPath \ "subject_types_supported").read[Seq[String]] and
      (JsPath \ "id_token_signing_alg_values_supported").read[Seq[String]] and
      (JsPath \ "scopes_supported").read[Seq[String]] and
      (JsPath \ "token_endpoint_auth_methods_supported").read[Seq[String]] and
      (JsPath \ "claims_supported").read[Seq[String]]
  )(GoogleOAuthConf.apply)
}
