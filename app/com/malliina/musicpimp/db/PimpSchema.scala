package com.malliina.musicpimp.db

import scala.slick.driver.H2Driver.simple.{Table, TableQuery}

object PimpSchema {
  val tracks = TableQuery[Tracks]
  val folders = TableQuery[Folders]
  val tokens = TableQuery[TokensTable]
  val usersTable = TableQuery[Users]
  val idsTable = TableQuery[Ids]
  val playlistsTable = TableQuery[PlaylistTable]
  val playlistTracksTable = TableQuery[PlaylistTracks]
  val plays = TableQuery[Plays]

  val tableQueries: Seq[TableQuery[_ <: Table[_]]] =
    Seq(plays, playlistTracksTable, playlistsTable, idsTable, tracks, folders, tokens, usersTable)
}
