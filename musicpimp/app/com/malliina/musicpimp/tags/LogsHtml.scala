package com.malliina.musicpimp.tags

import ch.qos.logback.classic.Level
import com.malliina.musicpimp.tags.PimpHtml.{feedbackDiv, postableForm}
import com.malliina.play.tags.All._
import controllers.musicpimp.{UserFeedback, routes}
import play.api.data.Field

import scalatags.Text.all._

object LogsHtml {
  def logsContent(levelField: Field,
                  levels: Seq[Level],
                  currentLevel: Level,
                  feedback: Option[UserFeedback]) = Seq(
    headerRow()("Logs"),
    feedback.fold(empty)(fb => feedbackDiv(fb)),
    rowColumn(ColMd4)(
      postableForm(routes.LogPage.changeLogLevel())(
        formGroup(
          select(`class` := FormControl, id := levelField.id, name := levelField.name, onchange := "this.form.submit()")(
            levels.map(level => option(if (level == currentLevel) selected else empty)(level.toString))
          )
        )
      )
    ),
    fullRow(
      headeredTable(s"$TableStripedHover $TableResponsive $TableCondensed",
        Seq("Time", "Message", "Logger", "Thread", "Level"))(
        tbody(id := "logTableBody")
      )
    )
  )
}
