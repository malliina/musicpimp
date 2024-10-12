package com.malliina.musicpimp.models

import com.malliina.musicpimp.audio.{FolderMeta, TrackMeta}

case class MusicColumn(folders: Seq[FolderMeta], tracks: Seq[TrackMeta]):
  val isEmpty = folders.isEmpty && tracks.isEmpty

object MusicColumn:
  val empty = MusicColumn(Nil, Nil)
