package com.malliina.musicpimp.models

import io.circe.Codec

case class FullSavedPlaylistsMeta(playlists: Seq[FullSavedPlaylist]) derives Codec.AsObject

case class PlaylistsMeta(playlists: Seq[SavedPlaylist])
