package com.malliina.musicpimp.tags

import com.malliina.musicpimp.tags.Bootstrap.HelpBlock
import com.malliina.musicpimp.tags.Tags._
import play.api.data.Field
import play.api.i18n.Messages

import scalatags.Text.all._

object PlayBootstrap extends PlayBootstrap

trait PlayBootstrap {
  def helpSpan(field: Field, m: Messages) = {
    field.error.map(error => Messages(error.message, error.args: _*)(m)).fold(empty) { formattedMessage =>
      spanClass(HelpBlock)(formattedMessage)
    }
  }
}