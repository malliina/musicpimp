package com.malliina.musicpimp.html

import com.malliina.musicpimp.scheduler.web.{AlarmStrings, SchedulerStrings}
import com.malliina.musicpimp.scheduler.web.SchedulerStrings._
import com.malliina.musicpimp.scheduler.{ClockPlayback, WeekDay}
import com.malliina.musicpimp.html.PlayBootstrap.helpSpan
import com.malliina.play.tags.All._
import controllers.musicpimp.{UserFeedback, routes}
import play.api.data.{Field, Form}
import play.api.i18n.Messages

import scalatags.Text.all._

object AlarmsHtml {
  def alarmsContent(clocks: Seq[ClockPlayback]) = Seq(
    headerRow()("Alarms"),
    fullRow(
      PimpHtml.stripedHoverTable(Seq("Description", "Enabled", "Actions"))(
        tbody(clocks.map(alarmRow))
      )
    ),
    fullRow(
      aHref(routes.Alarms.newAlarm())("Add alarm")
    )
  )

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
    divClass(BtnGroup)(
      aHref(routes.Alarms.editAlarm(id), `class` := s"$BtnDefault $BtnSm")(glyphIcon("edit"), " Edit"),
      aHref("#", dataToggle := Dropdown, `class` := s"$BtnDefault $BtnSm $DropdownToggle")(spanClass(Caret)),
      ulClass(DropdownMenu)(
        jsListElem(DeleteClass, id, "remove", "Delete"),
        jsListElem(PlayClass, id, "play", "Play"),
        jsListElem(StopClass, id, "stop", "Stop")
      )
    )

  def jsListElem(clazz: String, dataId: String, glyph: String, linkText: String) =
    liHref("#", `class` := clazz, PimpHtml.dataIdAttr := dataId)(glyphIcon(glyph), s" $linkText")

  def alarmEditorContent(form: Form[ClockPlayback],
                         feedback: Option[UserFeedback],
                         m: Messages) = Seq(
    headerRow()("Edit alarm"),
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
        feedback.fold(empty)(fb => divClass(s"$Lead $ColSmOffset2")(PimpHtml.feedbackDiv(fb)))
      )
    )
  )

  def saveButton(buttonText: String = "Save") =
    formGroup(
      divClass(s"$ColSmOffset2 $ColSm10")(
        defaultSubmitButton(buttonText)
      )
    )

  def weekdayCheckboxes(field: Field, messages: Messages) = {
    val errorClass = if (field.hasErrors) s" $HasError" else ""
    divClass(s"$FormGroup$errorClass")(
      labelFor(field.id, `class` := s"$ColSm2 $ControlLabel")("Days"),
      divClass(ColSm4, id := field.id)(
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
      divClass(s"$ColSmOffset2 $ColSm10")(
        divClass(Checkbox)(
          label(
            input(`type` := Checkbox, name := field.name, checkedAttr)(labelText)
          )
        )
      )
    )
  }

  def numberTextIn(field: Field, label: String, placeholderValue: String, m: Messages) =
    formTextIn(field, label, m, Option(placeholderValue), typeName = Number, inputWidth = ColSm2)

  def formTextIn(field: Field,
                 labelText: String,
                 m: Messages,
                 placeholder: Option[String] = None,
                 typeName: String = Text,
                 inputWidth: String = ColSm10,
                 inClasses: Seq[String] = Nil,
                 formGroupClasses: Seq[String] = Nil,
                 defaultValue: String = "") = {
    val errorClass = if (field.hasErrors) Option(HasError) else None
    val moreClasses = (errorClass.toSeq ++ formGroupClasses).mkString(" ", " ", "")
    val inputClasses = inClasses.mkString(" ", " ", "")
    divClass(s"$FormGroup$moreClasses")(
      labelFor(field.name, `class` := s"$ControlLabel $ColSm2")(labelText),
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
