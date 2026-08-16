package com.malliina.pimpcloud

import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.js.FrontStrings.EventKey
import com.malliina.musicpimp.models.*
import com.malliina.pimpcloud.CloudStrings.{Body, PhonesKey, RequestsKey, ServersKey}
import com.malliina.pimpcloud.ListEvent.format
import com.malliina.play.ContentRange
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Decoder, DecodingFailure, Encoder, Json}

sealed trait PimpList

object PimpList:
  given reader: Decoder[PimpList] =
    Decoder[PimpStreams]
      .map[PimpList](identity)
      .or(Decoder[PimpPhones].map[PimpList](identity))
      .or(Decoder[PimpServers].map[PimpList](identity))

  given Encoder[PimpList] =
    case ps @ PimpStreams(streams) => ps.asJson
    case pl @ PimpPhones(phones)   => pl.asJson
    case ps @ PimpServers(servers) => ps.asJson

case class PimpStream(
  request: RequestIdentifier,
  serverID: CloudID,
  track: Track,
  range: ContentRange
) derives Codec.AsObject

case class PimpStreams(streams: Seq[PimpStream]) extends PimpList

object PimpStreams:
  given json: Codec[PimpStreams] = format(RequestsKey, PimpStreams.apply)(_.streams)

case class PimpPhone(id: String, s: CloudID, address: String) derives Codec.AsObject

case class PimpPhones(phones: Seq[PimpPhone]) extends PimpList

object PimpPhones:
  given json: Codec[PimpPhones] = format(PhonesKey, PimpPhones.apply)(_.phones)

case class PimpServer(id: CloudID, address: String) derives Codec.AsObject

case class PimpServers(servers: Seq[PimpServer]) extends PimpList

object PimpServers:
  given json: Codec[PimpServers] = format(ServersKey, PimpServers.apply)(_.servers)

object ListEvent:
  def format[T: Codec, U](eventValue: String, build: Seq[T] => U)(strip: U => Seq[T]) =
    Codec.from(reader(eventValue, build), writer(eventValue, strip))

  def reader[T: Decoder, U](eventValue: String, build: Seq[T] => U): Decoder[U] =
    json =>
      json
        .downField(EventKey)
        .as[String]
        .flatMap:
          case `eventValue` =>
            json.downField(Body).as[Seq[T]].map(build)
          case other =>
            Left(
              DecodingFailure(
                s"Invalid '$EventKey' value of '$other', expected '$eventValue'.",
                Nil
              )
            )

  def writer[T: Encoder, U](eventValue: String, strip: U => Seq[T]): Encoder[U] =
    u => Json.obj(EventKey -> eventValue.asJson, Body -> strip(u).asJson)
