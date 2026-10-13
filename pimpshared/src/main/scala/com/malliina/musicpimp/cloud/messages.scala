package com.malliina.musicpimp.cloud

import com.malliina.http.Errors
import com.malliina.http4s.QueryParsers
import com.malliina.musicpimp.json.PimpStrings
import com.malliina.musicpimp.models.{FolderID, PlaylistID, TrackID}
import com.malliina.play.ContentRange
import com.malliina.values.Literals.nonNeg
import com.malliina.values.{NonNeg, Password, Username}
import io.circe.Codec
import org.http4s.{ParseFailure, Query, QueryParamDecoder}

trait PimpMessage

trait UserMessage extends PimpMessage:
  def username: Username

case object PingMessage extends PimpMessage
case object PongMessage extends PimpMessage
case object PingAuth extends PimpMessage
case object RootFolder extends PimpMessage

case class GetFolder(id: FolderID) extends PimpMessage derives Codec.AsObject

case class GetTrack(id: TrackID) extends PimpMessage derives Codec.AsObject

case class GetPlaylistById(id: PlaylistID) extends PimpMessage derives Codec.AsObject

case class DeletePlaylistById(id: PlaylistID) extends PimpMessage derives Codec.AsObject

case class RangedTrack(id: TrackID, range: ContentRange) extends PimpMessage derives Codec.AsObject

case class TrackRequest(id: TrackID, range: Option[ContentRange]) extends PimpMessage
  derives Codec.AsObject

case class Search(term: String, limit: Int) extends PimpMessage derives Codec.AsObject

object Search:
  given QueryParamDecoder[NonNeg] = QueryParamDecoder.intQueryParamDecoder.emap(int =>
    NonNeg(int).left.map(err => ParseFailure(err.message, err.message))
  )
  val DefaultSearchLimit: NonNeg = 100.nonNeg

  def apply(query: Query): Either[Errors, Search] =
    for
      q <- QueryParsers.parseOptE[String](query, PimpStrings.Term)
      term <- q.map(_.trim).filter(_.nonEmpty).toRight(Errors.single("Search term missing."))
      limit <- QueryParsers.parseOrDefault[NonNeg](query, PimpStrings.Limit, DefaultSearchLimit)
    yield Search(term, limit.value)

case object GetAlarms extends PimpMessage

case class Authenticate(username: Username, password: Password) extends PimpMessage
  derives Codec.AsObject

case class GetMeta(id: TrackID) extends PimpMessage derives Codec.AsObject
