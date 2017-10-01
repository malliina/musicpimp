package com.malliina.musicpimp.html

import com.malliina.musicpimp.scheduler.ClockPlayback
import com.malliina.play.controllers.AccountForms
import com.malliina.play.models.Username
import controllers.musicpimp.UserFeedback
import play.api.data.Form
import play.api.i18n.Messages

case class LoginConf(accounts: AccountForms,
                     motd: Option[String],
                     formFeedback: Option[UserFeedback],
                     topFeedback: Option[UserFeedback])

case class AlarmConf(form: Form[ClockPlayback],
                     feedback: Option[UserFeedback],
                     username: Username,
                     m: Messages)
