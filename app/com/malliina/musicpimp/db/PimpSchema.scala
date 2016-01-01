package com.malliina.musicpimp.db

import scala.slick.driver.H2Driver.simple._

/**
  * @author mle
  */
object PimpSchema {
  val tracks = TableQuery[Tracks]
  val folders = TableQuery[Folders]
  val tokens = TableQuery[TokensTable]
  val usersTable = TableQuery[Users]
  val idsTable = TableQuery[Ids]
  val playlistsTable = TableQuery[PlaylistTable]
  val playlistTracksTable = TableQuery[PlaylistTracks]

  val tableQueries: Seq[TableQuery[_ <: Table[_]]] = Seq(playlistTracksTable, playlistsTable, idsTable, tracks, folders, tokens, usersTable)
}
