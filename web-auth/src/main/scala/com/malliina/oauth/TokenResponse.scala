package com.malliina.oauth

import com.malliina.values.{AccessToken, IdToken}
import play.api.libs.json.{Json, OFormat}

case class TokenResponse(
  access_token: AccessToken,
  id_token: IdToken,
  expires_in: Long,
  token_type: String
)

object TokenResponse {
  implicit val json: OFormat[TokenResponse] = Json.format[TokenResponse]
}
