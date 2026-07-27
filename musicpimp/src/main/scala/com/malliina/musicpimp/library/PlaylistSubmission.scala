package com.malliina.musicpimp.library

import com.malliina.http.Errors
import com.malliina.http4s.FormReadableT
import com.malliina.musicpimp.http4s.{FormReaders, Playlists}
import com.malliina.musicpimp.models.{PlaylistID, TrackID}
import io.circe.Codec

case class PlaylistSubmission(id: Option[PlaylistID], name: String, tracks: Seq[TrackID])
  derives Codec.AsObject:
  val isUpdate = id.nonEmpty

object PlaylistSubmission extends FormReaders:
  given form: FormReadableT[PlaylistSubmission] = FormReadableT.reader.emap: form =>
    for
      id <- form.read[Option[PlaylistID]](Playlists.Id)
      name <- form
        .read[String](Playlists.Name)
        .filterOrElse(_.nonEmpty, Errors.single("Name cannot be empty."))
      tracks <- form.read[Seq[TrackID]](Playlists.Tracks)
    yield PlaylistSubmission(id, name, tracks)

case class SavePlaylistBody(playlist: PlaylistSubmission) derives Codec.AsObject

case class SimpleMessage(message: String) derives Codec.AsObject
