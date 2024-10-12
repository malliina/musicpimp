package com.malliina.musicpimp.db

import com.malliina.musicpimp.db.FullText.log
import io.getquill.*
import play.api.Logger

import scala.concurrent.Future

object FullText:
  private val log = Logger(getClass)

  def apply(db: PimpMySQL): FullText = new FullText(db)

class FullText(val db: PimpMySQL):
  import db.*

  val fullTextQuery: Quoted[String => Query[DataTrack]] = quote: (term: String) =>
    infix"SELECT T.ID,T.TITLE,T.ARTIST,T.ALBUM,T.DURATION,T.SIZE,T.PATH,T.FOLDER FROM TRACKS T WHERE MATCH(title, artist, album) AGAINST($term)"
      .as[Query[DataTrack]]

  def fullText(searchTerm: String, limit: Int = 1000): Future[List[DataTrack]] =
    wrapTask("Fulltext search"):
      log.debug(s"Querying: '$searchTerm'...")
      val words = searchTerm.split(" ")
      val commaSeparated = words.mkString(",")
      run(fullTextQuery(lift(commaSeparated)).take(lift(limit)))
