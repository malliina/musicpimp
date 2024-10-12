package com.malliina.musicpimp.messaging

import com.malliina.push.Token
import com.malliina.push.adm.ADMToken
import com.malliina.push.apns.APNSToken
import com.malliina.push.gcm.GCMToken
import com.malliina.push.mpns.MPNSToken
import com.malliina.values.{ErrorMessage, ValidatingCompanion}
import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Decoder, DecodingFailure, Encoder}

sealed abstract class TokenPlatform(val platform: String)

object TokenPlatform extends ValidatingCompanion[String, TokenPlatform]:
  val Key = "platform"
  val valids = Seq(Adm, Gcm, Mpns, Apns)

  override def build(input: String): Either[ErrorMessage, TokenPlatform] =
    valids.find(_.platform == input).toRight(defaultError(input))

  override def write(t: TokenPlatform): String = t.platform

case object Adm extends TokenPlatform("adm")
case object Gcm extends TokenPlatform("gcm")
case object Mpns extends TokenPlatform("mpns")
case object Apns extends TokenPlatform("apns")

case class TokenInfo(token: Token, platform: TokenPlatform)

object TokenInfo:
  val TokenKey = "token"

  val tokenReader = Decoder[Token]: json =>
    json
      .downField(TokenPlatform.Key)
      .as[TokenPlatform]
      .flatMap: platform =>
        val tokenJson = json.downField(TokenKey)
        platform match
          case Adm  => tokenJson.as[ADMToken]
          case Gcm  => tokenJson.as[GCMToken]
          case Mpns => tokenJson.as[MPNSToken]
          case Apns => tokenJson.as[APNSToken]
  implicit val tokenFormat: Codec[Token] =
    Codec.from[Token](tokenReader, Encoder[Token](t => t.token.asJson))
  implicit val json: Codec[TokenInfo] = deriveCodec[TokenInfo]

  def adm(token: ADMToken) = TokenInfo(token, Adm)

  def gcm(token: GCMToken) = TokenInfo(token, Gcm)

  def mpns(token: MPNSToken) = TokenInfo(token, Mpns)

  def apns(token: APNSToken) = TokenInfo(token, Apns)

case class Tokens(tokens: Seq[TokenInfo]) derives Codec.AsObject
