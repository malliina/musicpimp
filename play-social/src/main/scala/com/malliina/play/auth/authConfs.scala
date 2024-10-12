package com.malliina.play.auth

import java.nio.file.{Files, Path}

import cats.effect.IO
import com.malliina.http.HttpClient
import com.malliina.web.{AuthCodeConf, AuthConf, ClientId, ClientSecret, GenericAuthConf, KeyClient}
import play.api.Configuration
import play.api.mvc.Call

import scala.jdk.CollectionConverters.CollectionHasAsScala

object AuthConfReader:
  def env = apply(key => sys.env.get(key).orElse(sys.props.get(key)))

  def conf(c: Configuration) = apply(key => c.getOptional[String](key))

  def file(path: Path): AuthConfReader =
    val asMap = Files
      .readAllLines(path)
      .asScala
      .toList
      .filterNot(line => line.startsWith("#") || line.startsWith("//"))
      .map(line => line.split("=", 2))
      .collect:
        case Array(key, value) => key -> value
      .toMap
    apply(asMap.get)

  def apply(readKey: String => Option[String]): AuthConfReader =
    new AuthConfReader(readKey)

class AuthConfReader(readKey: String => Option[String]):
  def read(key: String): Either[String, String] =
    readKey(key).toRight(s"Key missing: '$key'.")

  def orFail(read: Either[String, AuthConf]) = read.fold(err => throw new Exception(err), identity)

  def github = readConf("github_client_id", "github_client_secret")
  def microsoft = readConf("microsoft_client_id", "microsoft_client_secret")
  def google = readConf("google_client_id", "google_client_secret")
  def facebook = readConf("facebook_client_id", "facebook_client_secret")
  def twitter = readConf("twitter_client_id", "twitter_client_secret")

  def readConf(clientIdKey: String, clientSecretKey: String): AuthConf =
    val attempt = for
      clientId <- read(clientIdKey).map(ClientId.apply)
      clientSecret <- read(clientSecretKey).map(ClientSecret.apply)
    yield AuthConf(clientId, clientSecret)
    orFail(attempt)

case class OAuthConf[U](
  redirCall: Call,
  handler: AuthResults[U],
  conf: AuthConf,
  http: HttpClient[IO]
) extends GenericAuthConf[IO]

case class CodeValidationConf[U](oauth: OAuthConf[U], codeConf: AuthCodeConf[IO]):
  def brandName = codeConf.brandName
  def handler = oauth.handler
  def conf: AuthConf = oauth.conf
  def client: KeyClient[IO] = codeConf.client
  def redirCall = oauth.redirCall
  def extraStartParams = codeConf.extraStartParams
  def extraValidateParams = codeConf.extraValidateParams
