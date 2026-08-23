package com.malliina.musicpimp.html

import ch.qos.logback.classic.Level
import com.malliina.html.HtmlTags.spanClass
import com.malliina.html.{HtmlTags, UserFeedback}
import com.malliina.musicpimp.scheduler.ClockPlaybackConf
import com.malliina.values.{ErrorMessage, Username}

case class LoginContent(
  accounts: AccountKeys,
  motd: Option[String],
  formFeedback: Option[UserFeedback],
  topFeedback: Option[UserFeedback]
)

case class AlarmContent(
  form: Option[ClockPlaybackConf],
  feedback: Option[UserFeedback],
  username: Username
) extends UserLike

case class LibraryContent(
  folders: Seq[String],
  folderPlaceholder: String,
  username: Username,
  feedback: Option[UserFeedback]
) extends UserLike

case class UsersContent(
  us: Seq[Username],
  username: Username,
  listFeedback: Option[UserFeedback],
  addFeedback: Option[UserFeedback]
) extends UserLike

trait UserLike:
  def username: Username

case class InField(id: String, name: String, value: Option[String], error: Option[ErrorMessage]):
  def hasErrors = error.isDefined
  def arrayName = s"$name[]"

  def valued(v: Option[String]): InField = copy(value = v)

object InField:
  def id(id: String): InField = InField(id, id, None, None)

  import scalatags.Text.all.*

  def helpSpan(field: InField): Modifier =
    field.error.fold(HtmlTags.empty): message =>
      spanClass("help-block")(message.message)

case class ChangeLogLevel(level: Level)

object ChangeLogLevel:
  val LevelKey = "level"
