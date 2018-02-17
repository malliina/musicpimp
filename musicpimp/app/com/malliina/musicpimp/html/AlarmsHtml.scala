package com.malliina.musicpimp.html

import com.malliina.musicpimp.html.PlayBootstrap.helpSpan
import com.malliina.musicpimp.messaging.TokenInfo
import com.malliina.musicpimp.scheduler.web.SchedulerStrings
import com.malliina.musicpimp.scheduler.web.SchedulerStrings._
import com.malliina.musicpimp.scheduler.{ClockPlayback, WeekDay}
import controllers.musicpimp.routes
import play.api.data.Field
import play.api.i18n.Messages

import scalatags.Text.all._

object AlarmsHtml extends PimpBootstrap {

  import tags._

  def tokens(tokens: Seq[TokenInfo]) = {
    val content =
      if (tokens.isEmpty) {
        leadPara("No push tokens.")
      } else {
        table(`class` := s"${tables.defaultClass} tokens-table")(
          thead(Seq(th("Token"), th(`class` := "token-header-platform")("Platform"))),
          tbody(tokens.map(t => tr(td(t.token.token), td(t.platform.platform))))
        )
      }
    Seq(
      headerRow("Tokens"),
      fullRow(content)
    )
  }

  def alarmsContent(clocks: Seq[ClockPlayback]) = {
    val content: Modifier =
      if (clocks.isEmpty) {
        leadPara("No alarms.")
      } else {
        PimpHtml.stripedHoverTableSmall(Seq("Description", "Enabled", "Actions"))(
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
      td(alarmActions(ap.id.getOrElse("nonexistent")))
    )
  }

  def alarmActions(id: String) =
    divClass(btn.group)(
      a(href := routes.Alarms.editAlarm(id), `class` := s"${btn.default} ${btn.sm}")(iconic("edit"), " Edit"),
      a(href := "#", dataToggle := Dropdown, `class` := s"${btn.default} ${btn.sm} $DropdownToggle")(spanClass(Caret)),
      ulClass(DropdownMenu)(
        jsListElem(DeleteClass, id, "remove", "Delete"),
        jsListElem(PlayClass, id, "play", "Play"),
        jsListElem(StopClass, id, "stop", "Stop")
      )
    )

  def jsListElem(clazz: String, dataId: String, glyph: String, linkText: String) =
    liHref("#", `class` := clazz, PimpHtml.dataIdAttr := dataId)(iconic(glyph), s" $linkText")

  def alarmEditorContent(conf: AlarmContent) = {
    val m = conf.m
    val form = conf.form
    Seq(
      headerRow("Edit alarm"),
      halfRow(
        PimpHtml.postableForm(routes.Alarms.newClock(), `class` := FormHorizontal)(
          divClass("hidden")(
            formTextIn(form(Id), "ID", m)
          ),
          numberTextIn(form(Hours), "Hours", "hh", m),
          numberTextIn(form(Minutes), "Minute", "mm", m),
          weekdayCheckboxes(form(Days), m),
          formTextIn(form(TrackId), "Track ID", m, formGroupClasses = Seq("hidden")),
          formTextIn(form(TrackKey), "Track", m, Option("Start typing the name of the track..."), inClasses = Seq(Selector)),
          checkField(form(Enabled), "Enabled"),
          saveButton(),
          conf.feedback.fold(empty)(fb => divClass(s"$Lead ${col.sm.offset.two}")(PimpHtml.feedbackDiv(fb)))
        )
      )
    )
  }

  def saveButton(buttonText: String = "Save") =
    formGroup(
      divClass(s"${col.sm.offset.two} ${col.sm.width("10")}")(
        defaultSubmitButton(buttonText)
      )
    )

  def weekdayCheckboxes(field: Field, messages: Messages) = {
    val errorClass = if (field.hasErrors) s" $HasError" else ""
    divClass(s"$FormGroup$errorClass")(
      labelFor(field.id, `class` := s"${col.sm.two} $ControlLabel")("Days"),
      divClass(col.sm.four, id := field.id)(
        divClass(Checkbox)(
          label(
            input(`type` := Checkbox, value := "every", id := Every)("Every day")
          )
        ),
        WeekDay.EveryDay.zipWithIndex.map { case (k, v) => dayCheckbox(field, k, v) },
        helpSpan(field, messages)
      )
    )
  }

  def dayCheckbox(field: Field, weekDay: WeekDay, index: Int) = {
    val isChecked = field.indexes.flatMap(i => field(s"[$i]").value).contains(weekDay.shortName)
    val checkedAttr = if (isChecked) checked else empty
    divClass(Checkbox)(
      label(
        input(
          `type` := Checkbox,
          value := field.value.getOrElse(weekDay.shortName),
          id := weekDay.shortName,
          name := s"${field.name}[$index]",
          checkedAttr
        )
      )(s" ${weekDay.longName}")
    )
  }

  def checkField(field: Field, labelText: String) = {
    val checkedAttr = if (field.value.contains(SchedulerStrings.On)) checked else empty
    formGroup(
      divClass(s"${col.sm.offset.two} ${col.sm.width("10")}")(
        divClass(Checkbox)(
          label(
            input(`type` := Checkbox, name := field.name, checkedAttr)(labelText)
          )
        )
      )
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
    val errorClass = if (field.hasErrors) Option(HasError) else None
    val moreClasses = (errorClass.toSeq ++ formGroupClasses).mkString(" ", " ", "")
    val inputClasses = inClasses.mkString(" ", " ", "")
    divClass(s"$FormGroup$moreClasses")(
      labelFor(field.name, `class` := s"$ControlLabel ${col.sm.two}")(labelText),
      divClass(inputWidth)(
        inputField(field, typeName, defaultValue, placeholder, `class` := s"$FormControl$inputClasses"),
        helpSpan(field, m)
      )
    )
  }

  def inputField(field: Field, typeName: String, defaultValue: String, placeHolder: Option[String], more: Modifier*) = {
    val placeholderAttr = placeHolder.fold(empty)(placeholder := _)
    input(`type` := typeName, id := field.id, name := field.name, value := field.value.getOrElse(defaultValue), placeholderAttr, more)
  }
}
