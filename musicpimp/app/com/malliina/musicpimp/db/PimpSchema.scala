package com.malliina.musicpimp.db

import slick.jdbc.H2Profile.api.{Table, TableQuery}

object PimpSchema {
  val tracks = TableQuery[Tracks]
  val folders = TableQuery[Folders]
  val tokens = TableQuery[TokensTable]
  val usersTable = TableQuery[Users]
  val tempFoldersTable = TableQuery[TempFolders]
  val tempTracksTable = TableQuery[TempTracks]
  val playlistsTable = TableQuery[PlaylistTable]
  val playlistTracksTable = TableQuery[PlaylistTracks]
  val plays = TableQuery[Plays]

  val tableQueries: Seq[TableQuery[_ <: Table[_]]] = Seq(
    plays, playlistTracksTable, playlistsTable,
    tempFoldersTable, tempTracksTable, tracks,
    folders, tokens, usersTable
  )
}
