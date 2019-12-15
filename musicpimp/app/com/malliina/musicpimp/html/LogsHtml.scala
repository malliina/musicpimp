package com.malliina.musicpimp.html

import ch.qos.logback.classic.Level
import com.malliina.musicpimp.html.PimpHtml.{feedbackDiv, postableForm}
import com.malliina.musicpimp.js.FrontStrings
import controllers.musicpimp.{UserFeedback, routes}
import play.api.data.Field

import scalatags.Text.all._

object LogsHtml extends PimpBootstrap with FrontStrings {
  val LogLevelClass = "log-level-select"

  import tags._

  def logsContent(
    levelField: Field,
    levels: Seq[Level],
    currentLevel: Level,
    feedback: Option[UserFeedback]
  ) = Seq(
    headerRow("Logs"),
    feedback.fold(empty)(fb => feedbackDiv(fb)),
    rowColumn(s"${col.sm.four} $PullRight $LogLevelClass")(
      postableForm(routes.LogPage.changeLogLevel())(
        formGroup(
          select(
            `class` := FormControl,
            id := levelField.id,
            name := levelField.name,
            onchange := "this.form.submit()"
          )(
            levels.map(level =>
              option(if (level == currentLevel) selected else empty)(level.toString)
            )
          )
        )
      )
    ),
    fullRow(
      headeredTable(tables.defaultClass, Seq("Time", "Message", "Logger", "Thread", "Level"))(
        tbody(id := LogTableBodyId)
      )
    )
  )
}
