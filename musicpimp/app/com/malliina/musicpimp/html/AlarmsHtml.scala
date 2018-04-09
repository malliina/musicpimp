package com.malliina.musicpimp.html

import com.malliina.musicpimp.html.PimpHtml.feedbackDiv
import com.malliina.musicpimp.html.PlayBootstrap.helpSpan
import com.malliina.musicpimp.messaging.TokenInfo
import com.malliina.musicpimp.scheduler.web.SchedulerStrings
import com.malliina.musicpimp.scheduler.web.SchedulerStrings._
import com.malliina.musicpimp.scheduler.{ClockPlayback, WeekDay}
import controllers.musicpimp.{UserFeedback, routes}
import play.api.data.Field
import play.api.i18n.Messages
import scalatags.Text.all._

object AlarmsHtml extends PimpBootstrap {

  import tags._

  def tokens(tokens: Seq[TokenInfo], feedback: Option[UserFeedback]) = {
    val content =
      if (tokens.isEmpty) {
        leadPara("No push tokens.")
      } else {
        modifier(
          feedback.fold(empty)(feedbackDiv),
          table(`class` := s"${tables.defaultClass} tokens-table")(
            thead(Seq(th("Token"), th(`class` := "token-header-platform")("Platform"), th("Actions"))),
            tbody(tokens.map(t => tr(td(t.token.token), td(t.platform.platform), td(`class` := "table-button")(removalForm(t)))))
          )
        )
      }
    Seq(
      headerRow("Tokens"),
      fullRow(content)
    )
  }

  def removalForm(tokenInfo: TokenInfo) =
    form(role := "form", action := routes.Alarms.remove(), method := "POST")(
      input(`type` := "hidden", name := "token", value := tokenInfo.token.token),
      input(`type` := "hidden", name := "platform", value := tokenInfo.platform.platform),
      button(`class` := s"${btn.danger} ${btn.sm}")(" Delete")
    )

  def alarmsContent(clocks: Seq[ClockPlayback]) = {
    val content: Modifier =
      if (clocks.isEmpty) {
        leadPara("No alarms.")
      } else {
        PimpHtml.stripedHoverTable(Seq("Description", "Enabled", "Actions"))(
          tbody(clocks.map(alarmRow))
        )
      }
    Seq(
      headerRow("Alarms"),
      fullRow(content),
      fullRow(a(href := routes.Alarms.newAlarm())("Add alarm"))
    )
  }

  def alarmRow(ap: ClockPlayback) = {
    val (enabledText, enabledAttr) =
      if (ap.enabled) ("Yes", empty)
      else ("No", `class` := "danger")
    tr(
      td(ap.describe),
      td(enabledAttr)(enabledText),
      td(`class` := "table-button")(alarmActions(ap.id.getOrElse("nonexistent")))
    )
  }

  def alarmActions(id: String) =
    divClass(btn.group)(
      a(href := routes.Alarms.editAlarm(id), `class` := s"${btn.secondary} ${btn.sm}")(iconic("edit"), " Edit"),
      button(`type` := Button, `class` := s"${btn.secondary} ${btn.sm} dropdown-toggle dropdown-toggle-split", dataToggle := Dropdown, aria.haspopup := True, aria.expanded := False),
      divClass(DropdownMenu)(
        jsListElem(DeleteClass, id, "delete", "Delete"),
        jsListElem(PlayClass, id, "play-circle", "Play"),
        jsListElem(StopClass, id, "media-stop", "Stop")
      )
    )

  def jsListElem(clazz: String, dataId: String, glyph: String, linkText: String) =
    a(href := "#", `class` := names(Seq("dropdown-item", clazz)), PimpHtml.dataIdAttr := dataId)(iconic(glyph), s" $linkText")

  def alarmEditorContent(conf: AlarmContent) = {
    val m = conf.m
    val form = conf.form
    Seq(
      headerRow("Edit alarm"),
      halfRow(
        PimpHtml.postableForm(routes.Alarms.newClock())(
          divClass("hide")(
            formTextIn(form(Id), "ID", m)
          ),
          numberTextIn(form(Hours), "Hours", "hh", m),
          numberTextIn(form(Minutes), "Minute", "mm", m),
          weekdayCheckboxes(form(Days), m),
          formTextIn(form(TrackId), "Track ID", m, formGroupClasses = Seq("hide")),
          formTextIn(form(TrackKey), "Track", m, Option("Start typing the name of the track..."), inClasses = Seq(Selector)),
          divClass(FormGroup)(enabledCheck(form(Enabled), "Enabled")),
          saveButton(),
          conf.feedback.fold(empty)(fb => PimpHtml.feedbackDiv(fb))
        )
      )
    )
  }

  def saveButton(buttonText: String = "Save") =
    divClass(FormGroup)(submitButton(`class` := btn.primary)(buttonText))

  def weekdayCheckboxes(field: Field, messages: Messages) = {
    val errorClass = if (field.hasErrors) s" $HasError" else ""
    divClass(s"$FormGroup$errorClass")(
      labelFor(field.id)("Days"),
      div(id := field.id)(
        checkField(Every, Option("every"), false, "Every day", Every),
        WeekDay.EveryDay.zipWithIndex.map { case (k, v) => dayCheckbox(field, k, v) },
        helpSpan(field, messages)
      )
    )
  }

  def dayCheckbox(field: Field, weekDay: WeekDay, index: Int) =
    checkField(
      s"${field.name}[$index]",
      field.value.orElse(Option(weekDay.shortName)),
      field.indexes.flatMap(i => field(s"[$i]").value).contains(weekDay.shortName),
      weekDay.longName,
      weekDay.shortName
    )

  def enabledCheck(field: Field, labelText: String) =
    formCheckField(field, field.value.contains(SchedulerStrings.On), labelText, "enabled-check")

  def formCheckField(field: Field, isChecked: Boolean, labelText: String, checkId: String) =
    checkField(field.name, field.value, isChecked, labelText, checkId)

  def checkField(checkName: String, checkValue: Option[String], isChecked: Boolean, labelText: String, checkId: String) = {
    val checkedAttr = if (isChecked) checked else empty
    val valueAttr = checkValue.map(value := _).getOrElse(empty)
    divClass("form-check")(
      input(`type` := Checkbox, `class` := "form-check-input", name := checkName, id := checkId, valueAttr, checkedAttr),
      label(`class` := "form-check-label", `for` := checkId)(labelText)
    )
  }

  def numberTextIn(field: Field, label: String, placeholderValue: String, m: Messages) =
    formTextIn(field, label, m, Option(placeholderValue), typeName = Number, inputWidth = col.sm.two)

  def formTextIn(field: Field,
                 labelText: String,
                 m: Messages,
                 placeholder: Option[String] = None,
                 typeName: String = Text,
                 inputWidth: String = col.sm.width("10"),
                 inClasses: Seq[String] = Nil,
                 formGroupClasses: Seq[String] = Nil,
                 defaultValue: String = "") = {
    val errorClass = if (field.hasErrors) Seq(HasError) else Nil
    divClass(names(Seq(FormGroup) ++ errorClass ++ formGroupClasses))(
      labelFor(field.id)(labelText),
      inputField(field, typeName, defaultValue, placeholder, `class` := names(Seq(FormControl) ++ inClasses)),
      helpSpan(field, m)
    )
  }

  def inputField(field: Field, typeName: String, defaultValue: String, placeHolder: Option[String], more: Modifier*) = {
    val placeholderAttr = placeHolder.fold(empty)(placeholder := _)
    input(`type` := typeName, id := field.id, name := field.name, value := field.value.getOrElse(defaultValue), placeholderAttr, more)
  }

  def names(ns: Seq[String]) = ns.mkString(" ")
}
