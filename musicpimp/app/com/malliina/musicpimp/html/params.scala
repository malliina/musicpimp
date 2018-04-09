package com.malliina.musicpimp.html

import com.malliina.musicpimp.scheduler.ClockPlaybackConf
import com.malliina.play.controllers.AccountForms
import com.malliina.play.models.Username
import controllers.musicpimp.UserFeedback
import play.api.data.Form
import play.api.i18n.Messages

case class LoginContent(accounts: AccountForms,
                        motd: Option[String],
                        formFeedback: Option[UserFeedback],
                        topFeedback: Option[UserFeedback])

case class AlarmContent(form: Form[ClockPlaybackConf],
                        feedback: Option[UserFeedback],
                        username: Username,
                        m: Messages) extends UserLike

case class LibraryContent(folders: Seq[String],
                          folderPlaceholder: String,
                          username: Username,
                          feedback: Option[UserFeedback]) extends UserLike

case class UsersContent(us: Seq[Username],
                        username: Username,
                        listFeedback: Option[UserFeedback],
                        addFeedback: Option[UserFeedback]) extends UserLike

trait UserLike {
  def username: Username
}
