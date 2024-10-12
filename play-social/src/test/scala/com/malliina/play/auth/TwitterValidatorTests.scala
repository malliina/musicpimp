package com.malliina.play.auth

import com.malliina.http.FullUrl
import com.malliina.web.ClientSecret
import com.malliina.web.TwitterAuthFlow.{percentEncode, sign, signingKey}

import scala.collection.SortedMap

/** @see
  *   http://wiki.oauth.net/TestCases
  */
class TwitterValidatorTests extends munit.FunSuite:
  test("encoding"):
    assert(percentEncode("http") == "http")

  test("encoding 2"):
    assert(percentEncode("&=*") == "%26%3D%2A")

  test("demo"):
    val key = signingKey(ClientSecret("cs"), None)
    assert(sign(key, "bs") == "egQqG5AJep5sJ7anhXju1unge2I=")

  test("demo 2"):
    val key = signingKey(ClientSecret("kd94hf93k423kf44"), Option("pfkkdhi9sl3r4s00"))
    val base =
      "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&file%3Dvacation.jpg%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3Dkllo9940pd9333jh%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1191242096%26oauth_token%3Dnnch734d00sl2jdk%26oauth_version%3D1.0%26size%3Doriginal"
    assert(sign(key, base) == "tR3+Ty81lMeYAr/Fid0kMTYa/WM=")

  test("base string"):
    val encodedParamsString = SortedMap(
      Seq(
        "oauth_signature_method" -> "RSA-SHA1",
        "oauth_version" -> "1.0",
        "oauth_consumer_key" -> "dpf43f3p2l4k3l03",
        "oauth_timestamp" -> "1196666512",
        "oauth_nonce" -> "13917289812797014437",
        "file" -> "vacaction.jpg",
        "size" -> "original"
      ).map:
        case (k, v) => (percentEncode(k), percentEncode(v))
      *
    ).map:
      case (k, v) => s"$k=$v"
    .mkString("&")
    val url = FullUrl("http", "photos.example.net", "/photos")
    val signatureBaseString = s"GET&${percentEncode(url.url)}&${percentEncode(encodedParamsString)}"
    val expected =
      "GET&http%3A%2F%2Fphotos.example.net%2Fphotos&file%3Dvacaction.jpg%26oauth_consumer_key%3Ddpf43f3p2l4k3l03%26oauth_nonce%3D13917289812797014437%26oauth_signature_method%3DRSA-SHA1%26oauth_timestamp%3D1196666512%26oauth_version%3D1.0%26size%3Doriginal"
    assert(signatureBaseString == expected)

  test("twitter guide") {
    val encodedParamsString = SortedMap(
      Seq(
        "status" -> "Hello Ladies + Gentlemen, a signed OAuth request!",
        "include_entities" -> "true",
        "oauth_consumer_key" -> "xvz1evFS4wEEPTGEFPHBog",
        "oauth_nonce" -> "kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg",
        "oauth_signature_method" -> "HMAC-SHA1",
        "oauth_timestamp" -> "1318622958",
        "oauth_token" -> "370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb",
        "oauth_version" -> "1.0"
      ).map:
        case (k, v) => (percentEncode(k), percentEncode(v))
      *
    ).map:
      case (k, v) => s"$k=$v"
    .mkString("&")
    val url = FullUrl("https", "api.twitter.com", "/1.1/statuses/update.json")
    val signatureBaseString =
      s"POST&${percentEncode(url.url)}&${percentEncode(encodedParamsString)}"
    val expected =
      "POST&https%3A%2F%2Fapi.twitter.com%2F1.1%2Fstatuses%2Fupdate.json&include_entities%3Dtrue%26oauth_consumer_key%3Dxvz1evFS4wEEPTGEFPHBog%26oauth_nonce%3DkYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1318622958%26oauth_token%3D370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb%26oauth_version%3D1.0%26status%3DHello%2520Ladies%2520%252B%2520Gentlemen%252C%2520a%2520signed%2520OAuth%2520request%2521"
    assert(signatureBaseString == expected)
    val key = signingKey(
      ClientSecret("kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw"),
      Option("LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE")
    )
    assert(sign(key, signatureBaseString) == "hCtSmYh+iHYCEqBWrE7C7hYmtUk=")
  }

  test("signature encoding"):
    assert(percentEncode("tnnArxj06cWHq44gCs1OSKk/jLY=") == "tnnArxj06cWHq44gCs1OSKk%2FjLY%3D")
