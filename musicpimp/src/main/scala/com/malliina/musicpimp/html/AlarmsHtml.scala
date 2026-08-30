package com.malliina.musicpimp.html

import com.malliina.html.UserFeedback
import com.malliina.musicpimp.html.PimpHtml.feedbackDiv
import com.malliina.musicpimp.messaging.TokenInfo
import com.malliina.musicpimp.scheduler.web.SchedulerStrings
import com.malliina.musicpimp.scheduler.web.SchedulerStrings.*
import com.malliina.musicpimp.scheduler.{FullClockPlayback, WeekDay}
import scalatags.Text.all.*

object AlarmsHtml extends HtmlSyntax:
  import tags.*

  def tokens(tokens: Seq[TokenInfo], feedback: Option[UserFeedback]) =
    val content =
      if tokens.isEmpty then leadPara("No push tokens.")
      else
        table(`class` := s"${tables.defaultClass} tokens-table")(
          thead(
            Seq(th("Token"), th(`class` := "token-header-platform")("Platform"), th("Actions"))
          ),
          tbody(
            tokens.map(t =>
              tr(
                td(t.token.token),
                td(t.platform.platform),
                td(`class` := "table-button")(removalForm(t))
              )
            )
          )
        )
    Seq(
      headerRow("Tokens"),
      fullRow(modifier(feedback.fold(empty)(feedbackDiv), content))
    )

  private def removalForm(tokenInfo: TokenInfo) =
    form(role := "form", action := reverse.manage.push.remove, method := "POST")(
      input(`type` := "hidden", name := "token", value := tokenInfo.token.token),
      input(`type` := "hidden", name := "platform", value := tokenInfo.platform.platform),
      button(`class` := s"${btn.danger} ${btn.sm}")(" Delete")
    )

  def alarmsContent(clocks: Seq[FullClockPlayback]) =
    val content: Modifier =
      if clocks.isEmpty then leadPara("No alarms.")
      else
        PimpHtml.stripedHoverTable(Seq("Description", "Enabled", "Actions"))(
          tbody(clocks.map(alarmRow))
        )
    Seq(
      headerRow("Alarms"),
      fullRow(content),
      fullRow(a(href := reverse.alarms.editor)("Add alarm"))
    )

  private def alarmRow(ap: FullClockPlayback) =
    val (enabledText, enabledAttr) =
      if ap.enabled then ("Yes", empty)
      else ("No", `class` := "danger")
    tr(
      td(ap.describe),
      td(enabledAttr)(enabledText),
      td(`class` := "table-button")(alarmActions(ap.id.getOrElse("nonexistent")))
    )

  private def alarmActions(id: String) =
    divClass(btn.group)(
      a(href := reverse.alarms.edit(id), `class` := s"${btn.secondary} ${btn.sm}")(
        iconic("edit"),
        " Edit"
      ),
      button(
        `type` := Button,
        `class` := s"${btn.secondary} ${btn.sm} dropdown-toggle dropdown-toggle-split",
        dataToggle := Dropdown,
        aria.haspopup := True,
        aria.expanded := False
      ),
      divClass(DropdownMenu)(
        jsListElem(DeleteClass, id, "delete", "Delete"),
        jsListElem(PlayClass, id, "play-circle", "Play"),
        jsListElem(StopClass, id, "media-stop", "Stop")
      )
    )

  private def jsListElem(clazz: String, dataId: String, glyph: String, linkText: String) =
    a(href := "#", `class` := names(Seq("dropdown-item", clazz)), PimpHtml.dataIdAttr := dataId)(
      iconic(glyph),
      s" $linkText"
    )

  def alarmEditorContent(conf: AlarmContent) =
    val form = conf.form
    Seq(
      headerRow("Edit alarm"),
      halfRow(
        PimpHtml.postableForm(reverse.alarms.add)(
          divClass("hide")(
            formTextIn(InField.id(Id).valued(form.flatMap(_.id)), "ID")
          ),
          numberTextIn(InField.id(Hours).valued(form.map(f => s"${f.when.hour}")), "Hours", "hh"),
          numberTextIn(
            InField.id(Minutes).valued(form.map(f => s"${f.when.minute}")),
            "Minute",
            "mm"
          ),
          weekdayCheckboxes(InField.id(Days), form.map(_.when.days).getOrElse(Nil)),
          formTextIn(
            InField.id(TrackId).valued(form.map(_.track).map(_.id)),
            "Track ID",
            formGroupClasses = Seq("hide")
          ),
          formTextIn(
            InField.id(TrackKey),
            "Track",
            Option("Start typing the name of the track..."),
            inClasses = Seq(Selector)
          ),
          divClass(FormGroup)(
            enabledCheck(InField.id(Enabled).valued(form.map(b => s"$b")), "Enabled")
          ),
          saveButton(),
          conf.feedback.fold(empty)(fb => PimpHtml.feedbackDiv(fb))
        )
      )
    )

  private def saveButton(buttonText: String = "Save") =
    divClass(FormGroup)(submitButton(`class` := btn.primary)(buttonText))

  private def weekdayCheckboxes(field: InField, checked: Seq[WeekDay]) =
    val errorClass = if field.hasErrors then s" $HasError" else ""
    divClass(s"$FormGroup$errorClass")(
      labelFor(field.id)("Days"),
      div(id := field.id)(
        checkField(Every, Option("every"), false, "Every day", Every),
        WeekDay.EveryDay.map: day =>
          dayCheckbox(field, day, checked.contains(day)),
        InField.helpSpan(field)
      )
    )

  private def dayCheckbox(field: InField, weekDay: WeekDay, isChecked: Boolean) =
    checkField(
      field.arrayName,
      field.value.orElse(Option(weekDay.shortName)),
      isChecked,
      weekDay.longName,
      weekDay.shortName
    )

  private def enabledCheck(field: InField, labelText: String) =
    formCheckField(field, field.value.contains(SchedulerStrings.On), labelText, "enabled-check")

  private def formCheckField(
    field: InField,
    isChecked: Boolean,
    labelText: String,
    checkId: String
  ) =
    checkField(field.name, field.value, isChecked, labelText, checkId)

  private def checkField(
    checkName: String,
    checkValue: Option[String],
    isChecked: Boolean,
    labelText: String,
    checkId: String
  ) =
    val checkedAttr = if isChecked then checked else empty
    val valueAttr = checkValue.map(value := _).getOrElse(empty)
    divClass("form-check")(
      input(
        `type` := Checkbox,
        `class` := "form-check-input",
        name := checkName,
        id := checkId,
        valueAttr,
        checkedAttr
      ),
      label(`class` := "form-check-label", `for` := checkId)(labelText)
    )

  private def numberTextIn(field: InField, label: String, placeholderValue: String) =
    formTextIn(
      field,
      label,
      Option(placeholderValue),
      typeName = Number,
      inputWidth = col.sm.two
    )

  private def formTextIn(
    field: InField,
    labelText: String,
    placeholder: Option[String] = None,
    typeName: String = Text,
    inputWidth: String = col.sm.width("10"),
    inClasses: Seq[String] = Nil,
    formGroupClasses: Seq[String] = Nil,
    defaultValue: String = ""
  ) =
    val errorClass = if field.hasErrors then Seq(HasError) else Nil
    divClass(names(Seq(FormGroup) ++ errorClass ++ formGroupClasses))(
      labelFor(field.id)(labelText),
      inputField(
        field,
        typeName,
        defaultValue,
        placeholder,
        `class` := names(Seq(FormControl) ++ inClasses)
      ),
      InField.helpSpan(field)
    )

  private def inputField(
    field: InField,
    typeName: String,
    defaultValue: String,
    placeHolder: Option[String],
    more: Modifier*
  ) =
    val placeholderAttr = placeHolder.fold(empty)(placeholder := _)
    input(
      `type` := typeName,
      id := field.id,
      name := field.name,
      value := field.value.getOrElse(defaultValue),
      placeholderAttr,
      more
    )

  private def names(ns: Seq[String]) = ns.mkString(" ")
