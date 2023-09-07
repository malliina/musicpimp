package com.malliina.musicpimp.messaging

import com.malliina.push.Token
import com.malliina.push.adm.ADMToken
import com.malliina.push.apns.APNSToken
import com.malliina.push.gcm.GCMToken
import com.malliina.push.mpns.MPNSToken
import com.malliina.values.{ErrorMessage, ValidatingCompanion}
import play.api.libs.json._

sealed abstract class TokenPlatform(val platform: String)

object TokenPlatform extends ValidatingCompanion[String, TokenPlatform] {
  val Key = "platform"
  val valids = Seq(Adm, Gcm, Mpns, Apns)

  override def build(input: String): Either[ErrorMessage, TokenPlatform] =
    valids.find(_.platform == input).toRight(defaultError(input))

  override def write(t: TokenPlatform): String = t.platform
}

case object Adm extends TokenPlatform("adm")

case object Gcm extends TokenPlatform("gcm")

case object Mpns extends TokenPlatform("mpns")

case object Apns extends TokenPlatform("apns")

case class TokenInfo(token: Token, platform: TokenPlatform)

object TokenInfo {
  val TokenKey = "token"

  val tokenReader = Reads[Token] { json =>
    (json \ TokenPlatform.Key).validate[TokenPlatform].flatMap { platform =>
      val tokenJson = json \ TokenKey
      platform match {
        case Adm  => tokenJson.validate[ADMToken]
        case Gcm  => tokenJson.validate[GCMToken]
        case Mpns => tokenJson.validate[MPNSToken]
        case Apns => tokenJson.validate[APNSToken]
        case _    => JsError(s"Unsupported platform: '$platform'.")
      }
    }
  }
  implicit val tokenFormat: Format[Token] = Format[Token](tokenReader, Writes[Token](t => Json.toJson(t.token)))
  implicit val json: OFormat[TokenInfo] = Json.format[TokenInfo]

  def adm(token: ADMToken) = TokenInfo(token, Adm)

  def gcm(token: GCMToken) = TokenInfo(token, Gcm)

  def mpns(token: MPNSToken) = TokenInfo(token, Mpns)

  def apns(token: APNSToken) = TokenInfo(token, Apns)
}

case class Tokens(tokens: Seq[TokenInfo])

object Tokens {
  implicit val json: OFormat[Tokens] = Json.format[Tokens]
}
