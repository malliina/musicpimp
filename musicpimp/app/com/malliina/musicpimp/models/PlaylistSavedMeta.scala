package com.malliina.musicpimp.models

import io.circe.Codec

case class PlaylistSavedMeta(id: PlaylistID) derives Codec.AsObject
