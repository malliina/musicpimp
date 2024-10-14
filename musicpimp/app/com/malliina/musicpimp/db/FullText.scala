package com.malliina.musicpimp.db

import com.malliina.database.DoobieDatabase
import com.malliina.musicpimp.db.FullText.log
import doobie.implicits.*
import play.api.Logger

object FullText:
  private val log = Logger(getClass)

class FullText[F[_]](val db: DoobieDatabase[F]) extends DoobieMappings:
  def fullText(searchTerm: String, limit: Int = 1000): F[List[DataTrack]] = db.run:
    log.debug(s"Querying: '$searchTerm'...")
    val words = searchTerm.split(" ")
    val commaSeparated = words.mkString(",")
    sql"""select T.ID,T.TITLE,T.ARTIST,T.ALBUM,T.DURATION,T.SIZE,T.PATH,T.FOLDER
          from TRACKS T
          where MATCH(title, artist, album) AGAINST($commaSeparated)
          limit $limit"""
      .query[DataTrack]
      .to[List]
