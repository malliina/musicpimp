package com.malliina.musicpimp.json

object JsonStrings extends JsonStrings

trait JsonStrings extends PimpStrings with PlaybackStrings with CommonStrings {
  val Ok = "ok"

  val Failure = "failure"
  val State = "state"
  val AccessDenied = "Access denied"
  val InvalidParameter = "Invalid parameter"
  val InvalidCredentials = "Invalid credentials"
  val InvalidJson = "Invalid JSON"
  val NoFileInMultipart = "No file in multipart/form-data"
  val DatabaseError = "A database error occurred"
  val GenericError = "A generic error occurred"

  val Parent = "parent"
  val Contents = "contents"

  val Gain = "gain"
  val IsDir = "is_dir"
  val Playlist = "playlist"
  //  @deprecated("Use POSITION", "1.8.0")
  val Pos = "pos"
  val Position = "position"
  //  @deprecated("Use POSITION", "1.8.0")
  val PosSeconds = "pos_seconds"
  //  @deprecated("Use DURATION", "1.8.0")
  val DurationSeconds = "duration_seconds"
  val Msg = "msg"
  val ThankYou = "thank you"

  val TrackHeader = "Track"

  val Job = "job"
  val When = "when"
  val Enabled = "enabled"

  // push notifications
  val Push = "push"
  val Result = "result"
}
