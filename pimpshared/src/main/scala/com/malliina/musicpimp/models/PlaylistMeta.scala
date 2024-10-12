package com.malliina.musicpimp.models

import io.circe.Codec

case class FullPlaylistMeta(playlist: FullSavedPlaylist) derives Codec.AsObject

case class PlaylistMeta(playlist: SavedPlaylist)
