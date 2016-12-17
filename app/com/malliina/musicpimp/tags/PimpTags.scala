package com.malliina.musicpimp.tags

import ch.qos.logback.classic.Level
import com.malliina.musicpimp.BuildInfo
import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.db.DataTrack
import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models._
import com.malliina.musicpimp.scheduler.web.SchedulerStrings._
import com.malliina.musicpimp.scheduler.{ClockPlayback, WeekDay}
import com.malliina.musicpimp.stats.{PopularEntry, RecentEntry, TopEntry}
import com.malliina.musicpimp.tags.Bootstrap._
import com.malliina.musicpimp.tags.PimpTags.callAttr
import com.malliina.musicpimp.tags.Tags._
import com.malliina.play.auth.RememberMeCredentials
import com.malliina.play.controllers.AccountForms
import com.malliina.play.models.{PasswordChange, Username}
import controllers.routes.Assets.at
import controllers.{Accounts, Cloud, routes}
import play.api.data.{Field, Form}
import play.api.i18n.Messages
import play.api.mvc.{Call, Flash}

import scalatags.Text.all._
import scalatags.Text.{GenericAttr, TypedTag}

object PimpTags {
  implicit val callAttr = new GenericAttr[Call]

  def forApp(isProd: Boolean): PimpTags = {
    val scripts = ScalaScripts.forApp(BuildInfo.name, isProd)
    new PimpTags(scripts.optimized, scripts.launcher)
  }

  def withJs(jsFiles: String*): PimpTags =
    new PimpTags(jsFiles.map(file => jsScript(at(file))): _*)
}

class PimpTags(scripts: Modifier*) {

  val alarmJs = jsLink("scheduler.js")

  val FormSignin = "form-signin"
  val ariaLabel = attr("aria-label")
  val True = "true"

  def users(us: Seq[Username], addForm: Form[NewUser], username: Username, flash: Flash) =
    manage("users", username)(
      row(
        divClass(ColMd6)(
          divClass(PageHeader)(
            h1("Users")
          ),
          pimpTable(Seq("Username", "Actions"))(
            tbody(
              us map { u =>
                tr(
                  td(u.name),
                  td(postableForm(routes.Accounts.delete(u))(button(`class` := s"$BtnDanger $BtnXs")(" Delete")))
                )
              }
            )
          ),
          flash.get(Accounts.UsersFeedback).fold(empty) { feedback =>
            alertDiv(AlertDanger, feedback)
          }
        ),
        divClass(ColMd4)(
          divClass(PageHeader)(
            h1("Add user")
          ),
          addUser(addForm, flash)
        )
      )
    )

  def search(query: Option[String], results: Seq[DataTrack], username: Username) =
    libraryBase("search", username, jsLinks("sarch.js", "json.js", "library.js"))(
      headerRow()("Search"),
      row(
        divClass(ColMd4)(
          searchForm(None, "")
        ),
        divClass(s"$ColMd4 $ColMdOffset4")(
          onClickButton(s"$BtnDefault $BtnLg", "refresh()", "refresh"),
          span(id := "index-info")
        )
      ),
      fullRow(
        if (results.nonEmpty) {
          responsiveTable(results)("Track", "Artist", "Album", "Actions") { track =>
            Seq(td(track.title), td(track.artist), td(track.album), td(trackActions(track.id, Option("flex"))()))
          }
        } else {
          query.fold(empty) { term =>
            h3(s"No results for '$term'.")
          }
        }
      )
    )

  def playlist(playlist: SavedPlaylist, form: Form[PlaylistSubmission], username: Username) =
    indexMain("playlist", username)(
      headerRow()("Playlist"),
      leadPara(playlist.name),
      fullRow(
        if (playlist.tracks.isEmpty) {
          leadPara("This playlist is empty.")
        } else {
          responsiveTable(playlist.tracks)("Title", "Album", "Artist") { track =>
            Seq(td(track.title), td(track.album), td(track.artist))
          }
        }
      )
    )

  def responsiveTable[T](entries: Seq[T])(headers: String*)(cells: T => Seq[Modifier]) =
    headeredTable(TableStripedHoverResponsive, headers.map(stringFrag))(
      tbody(entries.map(entry => tr(cells(entry))))
    )

  def player(feedback: Option[String], username: Username) =
    basePlayer(feedback, username, jsLinks("json.js", "playerws.js"))

  def musicFolders(folders: Seq[String], newFolderForm: Form[String], folderPlaceholder: String, username: Username, m: Messages) =
    manage("folders", username)(
      headerRow(ColMd8)("Music Folders"),
      editFolders(folders, newFolderForm, folderPlaceholder, m)
    )

  def mostRecent(entries: Seq[RecentEntry], username: Username) =
    topList[RecentEntry]("recent", username, "Most recent", entries, "When", _.whenFormatted)

  def mostPopular(entries: Seq[PopularEntry], username: Username) =
    topList[PopularEntry]("popular", username, "Most popular", entries, "Plays", _.playbackCount)

  def topList[T <: TopEntry](tab: String,
                             username: Username,
                             headerText: String,
                             entries: Seq[T],
                             fourthHeader: String,
                             fourthValue: T => Modifier) =
    libraryBase(tab, username, jsLinks("json.js", "library.js"))(
      headerRow()(headerText, small(`class` := HiddenXs)(s" by ${username.name}")),
      fullRow(
        responsiveTable(entries)("Title", "Artist", "Album", fourthHeader, "Actions") { entry =>
          Seq(
            td(entry.track.title),
            td(entry.track.artist),
            td(entry.track.album),
            td(fourthValue(entry)),
            td(trackActions(entry.track.id, Option("flex"))())
          )
        }
      )
    )

  def logs(levelField: Field, levels: Seq[Level], currentLevel: Level, username: Username, errorMsg: Option[String]) =
    manage("logs", username, jsLink("rx.js"))(
      headerRow()("Logs"),
      errorMsg.fold(empty)(msg => fullRow(leadPara(msg))),
      rowColumn(ColMd4)(
        postableForm(routes.LogPage.changeLogLevel())(
          divClass(FormGroup)(
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

  def login(accounts: AccountForms, loginForm: Form[RememberMeCredentials], motd: Option[String], flash: Flash) =
    basePage("Welcome", cssLink(at("css/login.css")))(
      divContainer(
        rowColumn(s"$ColMd4 $FormSignin")(
          flash.get(accounts.feedback).fold(empty) { message =>
            alertDiv(AlertSuccess, message)
          }
        ),
        rowColumn(ColMd4)(
          postableForm(routes.Accounts.formAuthenticate(), `class` := FormSignin, name := "loginForm")(
            h2("Please sign in"),
            divClass(FormGroup)(
              textInputBase(Text, accounts.userFormKey, Option("Username"), `class` := FormControl, autofocus)
            ),
            divClass(FormGroup)(
              textInputBase(Password, accounts.passFormKey, Option("Password"), `class` := FormControl)
            ),
            divClass(Checkbox)(
              label(
                input(`type` := Checkbox, value := True, name := accounts.rememberMeKey, id := accounts.rememberMeKey),
                " Remember me"
              )
            ),
            blockSubmitButton()("Sign in")
          )
        ),
        rowColumn(ColMd4)(
          loginForm.globalError.fold(empty) { error =>
            alertDiv(s"$AlertWarning $FormSignin", error.message)
          }
        ),
        rowColumn(s"$ColMd4 $FormSignin")(
          motd.fold(empty)(message => p(message))
        )
      )
    )

  def library(relativePath: PimpPath, col1: MusicColumn, col2: MusicColumn, col3: MusicColumn, username: Username) =
    libraryBase("folders", username, jsLinks("json.js", "library.js"))(
      div(PageHeader)(
        h1("Library ", small(relativePath.path))
      ),
      row(
        renderColumn(col1, onlyColumn = col2.isEmpty && col3.isEmpty),
        renderColumn(col2),
        renderColumn(col3)
      ),
      if (col1.isEmpty && col2.isEmpty && col3.isEmpty && relativePath.path.isEmpty) {
        leadPara("The library is empty. To get started, add music folders under ",
          aHref(routes.SettingsController.settings())("Music Folders"), ".")
      } else {
        empty
      }
    )

  def renderColumn(col: MusicColumn, onlyColumn: Boolean = false) =
    divClass(if (onlyColumn) ColMd10 else ColMd4)(
      ul(ListUnstyled)(
        col.folders.map(f => Seq[Modifier](folderActions(f.id), " ", aHref(routes.LibraryController.library(f.id))(f.title))),
        col.tracks.map(t => li(`class` := Lead)())
      )
    )

  def jsLinks(files: String*) = files map jsLink

  def jsLink(file: String) = jsScript(at(s"js/$file"))

  def libraryBase(tab: String, username: Username, extraHeader: Modifier*)(inner: Modifier*) =
    indexMain("library", username, extraHeader)(
      ulClass(NavTabs)(
        iconNavItem("Folders", "folders", tab, routes.LibraryController.rootLibrary(), "fa fa-folder-open"),
        iconNavItem("Most Played", "popular", tab, routes.Website.popular(), "fa fa-list"),
        iconNavItem("Most Recent", "recent", tab, routes.Website.recent(), "fa fa-clock-o"),
        iconNavItem("Search", "search", tab, routes.SearchPage.search(), "fa fa-search")
      ),
      section(inner)
    )

  def folderActions(folder: FolderID) =
    musicItemActions(s"return playItems('$folder');", s"return addItems('$folder');", None, ariaLabel := "folder action")()

  def titledTrackActions(track: TrackMeta) =
    trackActions(track.id)(
      onClickButtonBase(BtnDefault, s"return play('${track.id}');")(track.title)
    )

  def trackActions(track: TrackID, extraClass: Option[String] = None)(inner: Modifier*) =
    musicItemActions(s"return play('$track');", s"return add('$track');", extraClass)(inner)

  def musicItemActions(onPlay: String, onAdd: String, extraClass: Option[String], groupAttrs: Modifier*)(inner: Modifier*) = {
    val extra = extraClass.map(c => s" $c").getOrElse(empty)
    divClass(s"$BtnGroup$extra", role := "group", groupAttrs)(
      onClickButton(BtnPrimary, onAdd, "play"),
      onClickButton(BtnDefault, onPlay, "plus"),
      inner
    )
  }

  def onClickButton(clazz: String, onClicked: String, glyph: String) =
    onClickButtonBase(clazz, onClicked)(glyphIcon(glyph))

  def onClickButtonBase(clazz: String, onClicked: String) =
    button(`type` := Button, `class` := clazz, onclick := onClicked)

  def editFolders(folders: Seq[String], newFolderForm: Form[String], folderPlaceholder: String, messages: Messages) =
    halfRow(
      ulClass(ListUnstyled)(
        folders.map(renderFolder)
      ),
      postableForm(routes.SettingsController.newFolder(), `class` := FormHorizontal, name := "newFolderForm")(
        divClass(InputGroup)(
          spanClass(InputGroupAddon)(glyphIcon("folder-open")),
          textInputBase(Text, "path", Option(folderPlaceholder), `class` := FormControl, required),
          spanClass(InputGroupBtn)(
            button(`type` := Submit, `class` := BtnPrimary)(glyphIcon("plus"), " Add")
          )
        ),
        newFolderForm.errors.map(error => pClass("error")(Messages(error.message)(messages)))
      )
    )

  def renderFolder(folder: String) =
    postableForm(routes.SettingsController.deleteFolder(folder), `class` := FormHorizontal)(
      divClass(InputGroup)(
        spanClass(InputGroupAddon)(glyphIcon("folder-open")),
        spanClass(s"$UneditableInput $FormControl")(folder),
        spanClass(InputGroupBtn)(
          button(`type` := Submit, `class` := BtnDefault)(glyphIcon("remove"), " Delete")
        )
      )
    )

  def cloud(c: Cloud, cloudForm: Form[Option[String]], serverId: Option[String], feedback: Option[String], username: Username, flash: Flash) =
    manage("cloud", username)(
      headerRow()("Cloud"),
      halfRow(
        toggleButton(serverId.map(_ => "Disconnect").getOrElse("Connect"), c, serverId)
      ),
      serverId.fold(empty) { server =>
        halfRow {
          leadPara(s"Connected. You can now access this server using your credentials and this cloud ID: $server")
        }
      },
      feedback.fold(empty) { fb =>
        halfRow(
          pClass(s"$Lead error")(fb)
        )
      },
      flash.get(c.FEEDBACK).fold(empty) { message =>
        leadPara(message)
      },
      halfRow(
        p("How does this work?"),
        p("This server will open a connection to a machine on the internet. Your mobile device connects to the " +
          "same machine on the internet and communicates with this server through the machine both parties have " +
          "connected to. All traffic is encrypted. All music is streamed.")
      )
    )

  def toggleButton(title: String, c: Cloud, serverId: Option[String]) =
    postableForm(routes.Cloud.toggle(), name := "toggleForm")(
      if (serverId.isEmpty) {
        divClass(FormGroup)(
          labelFor(c.idFormKey)("Desired cloud ID (optional"),
          textInputBase(Text, c.idFormKey, Option("Your desired ID or leave empty"))
        )
      } else {
        empty
      },
      blockSubmitButton(id := "toggleButton")(title)
    )

  def basePlayer(feedback: Option[String], username: Username, scripts: Modifier*) =
    indexMain("player", username, scripts ++ Seq(jsLink("sliders.js")))(
      headerRow(ColMd9)("Player"),
      div(id := "playerDiv", style := "display: none")(
        feedback.fold(empty) { fb =>
          rowColumn(ColMd9)(
            pClass(s"$Lead $Alert", id := "feedback")(fb)
          )
        },
        halfRow(
          pClass(Lead, id := "notracktext")(
            "No track. Play one from the ",
            aHref(routes.LibraryController.rootLibrary())("library"),
            " or stream from a mobile device."
          )
        ),
        row(
          playerControls,
          divClass(ColMd3)(
            h2("Playlist"),
            pClass(Lead, id := "empty_playlist_text")("The playlist is empty."),
            ol(id := "playlist")
          )
        )
      )
    )

  def playerControls = {
    val centerAttr = style := "text-align: center"
    divClass(ColMd6)(
      fullRow(
        h2(id := "title", centerAttr)("No track"),
        h4(id := "album", centerAttr),
        h3(id := "artist", centerAttr)
      ),
      fullRow(
        div(ColMd11)(
          div(id := "slider")
        ),
        divClass(s"$Row $ColMd11", id := "progress")(
          span(id := "pos")("00:00"), " / ", span(id := "duration")("00:00")
        ),
        divClass(s"$Row $ColMd11 text-center")(
          imageInput(at("img/transport.rew.png"), onclick := "prev()"),
          imageInput(at("img/light/transport.play.png"), id := "playButton", onclick := "resume()"),
          imageInput(at("img/light/transport.pause.png"), id := "pauseButton", style := "display: none", onclick := "stop()"),
          imageInput(at("img/light/transport/ff.png"), onclick := "next()")
        ),
        divClass(s"$Row $ColMd11 $VisibleLg")(
          divClass(ColMd3)(
            imageInput(at("img/light/appbar.sound.3.png"), `class` := PullRight, onclick := "togglemute()")
          ),
          divClass(ColMd8)(
            divClass(s"$ColMd12 $PullLeft", id := "volume")
          )
        )
      )
    )
  }

  def imageInput[V: AttrValue](imageUrl: V, more: Modifier*) =
    input(`type` := Image, src := imageUrl, more)

  def alarms(clocks: Seq[ClockPlayback], username: Username) =
    manage("alarms", username, alarmJs)(
      headerRow()("Alarms"),
      fullRow(
        pimpTable(Seq("Description", "Enabled", "Actions"))(
          tbody(clocks.map(alarmRow))
        )
      ),
      fullRow(
        aHref(routes.Alarms.newAlarm())("Add alarm")
      )
    )

  def alarmRow(ap: ClockPlayback) = {
    val (enabledText, enabledAttr) = if (ap.enabled) ("Yes", empty) else ("No", `class` := "danger")
    tr(td(ap.describe), td(enabledAttr)(enabledText), td(alarmActions(ap.id.getOrElse("nonexistent"))))
  }

  def pimpTable(headers: Seq[Modifier])(tableBody: Modifier*) =
    headeredTable(TableStripedHover, headers)(tableBody)

  def headeredTable(clazz: String, headers: Seq[Modifier])(tableBody: Modifier*) =
    table(`class` := clazz)(
      thead(headers.map(header => th(header))),
      tableBody
    )

  def alarmActions(id: String) =
    divClass(BtnGroup)(
      aHref(routes.Alarms.editAlarm(id), `class` := s"$BtnDefault $BtnSm")(glyphIcon("edit"), " Edit"),
      aHref("#", dataToggle := Dropdown, `class` := s"$BtnDefault $BtnSm $DropdownToggle")(spanClass(Caret)),
      ulClass(DropdownMenu)(
        jsListElem(s"deleteAP('$id')", "remove", "Delete"),
        jsListElem(s"runAP('$id')", "play", "Play"),
        jsListElem(s"stopPlayback()", "stop", "Stop")
      )
    )

  def jsListElem(onClicked: String, glyph: String, linkText: String) =
    liHref("#", onclick := onClicked)(glyphIcon(glyph), s" $linkText")

  def alarmEditor(form: Form[ClockPlayback], feedback: Option[String], username: Username, m: Messages) =
    manage("alarms", username, alarmJs, jsLink("alarm-editor.js"))(
      headerRow()("Edit alarm"),
      halfRow(
        postableForm(routes.Alarms.newClock(), `class` := FormHorizontal)(
          divClass("hidden")(
            formTextIn(form(ID), "ID", m)
          ),
          numberTextIn(form(HOURS), "Hours", "hh", m),
          numberTextIn(form(MINUTES), "Minute", "mm", m),
          weekdayCheckboxes(form(DAYS), m),
          formTextIn(form(TRACK_ID), "Track ID", m, formGroupClasses = Seq("hidden")),
          formTextIn(form(TRACK), "Track", m, Option("Start typing the name of the track..."), inClasses = Seq("selector")),
          checkField(form(ENABLED), "Enabled"),
          saveButton(),
          feedback.fold(empty)(fb => pClass(s"$Lead $ColSmOffset2")(fb))
        )
      )
    )

  def checkField(field: Field, labelText: String) = {
    val checkedAttr = if (field.value.contains("on")) checked else empty
    divClass(FormGroup)(
      divClass(s"$ColSmOffset2 $ColSm10")(
        divClass(Checkbox)(
          label(
            input(`type` := Checkbox, name := field.name, checkedAttr)(labelText)
          )
        )
      )
    )
  }

  def weekdayCheckboxes(field: Field, messages: Messages) = {
    val errorClass = if (field.hasErrors) s" $HasError" else ""
    divClass(s"$FormGroup$errorClass")(
      labelFor(field.id, `class` := s"$ColSm2 $ControlLabel")("Days"),
      divClass(ColSm4, id := field.id)(
        divClass(Checkbox)(
          label(
            input(`type` := Checkbox, value := "every", id := "every", onclick := "everyDayClicked()")("Every day")
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
    divClass("checkbox")(
      label(
        input(`type` := Checkbox,
          value := field.value.getOrElse(weekDay.shortName),
          id := weekDay.shortName,
          name := s"${field.name}[$index]",
          onclick := "updateEveryDayCheckbox()",
          checkedAttr
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

  def textInput(inType: String, clazz: String, idAndName: String, placeHolder: Option[String], more: Modifier*) =
    textInputBase(inType, idAndName, placeHolder, `class` := clazz, more)

  def textInputBase(inType: String, idAndName: String, placeHolder: Option[String], more: Modifier*) = {
    val placeholderAttr = placeHolder.fold(empty)(placeholder := _)
    input(`type` := inType, name := idAndName, id := idAndName, placeholderAttr, more)
  }

  def helpSpan(field: Field, m: Messages) = {
    field.error.map(error => Messages(error.message, error.args: _*)(m)).fold(empty) { formattedMessage =>
      spanClass("help-block")(formattedMessage)
    }
  }

  def manage(tab: String, username: Username, extraHeader: Modifier*)(inner: Modifier*) =
    indexMain("manage", username, extraHeader)(
      ulClass(NavTabs)(
        glyphNavItem("Music Folders", "folders", tab, routes.SettingsController.settings(), "folder-open"),
        glyphNavItem("Users", "users", tab, routes.Accounts.users(), "user"),
        glyphNavItem("Alarms", "alarms", tab, routes.Alarms.alarms(), "time"),
        glyphNavItem("Cloud", "cloud", tab, routes.Cloud.cloud(), "cloud"),
        glyphNavItem("Logs", "logs", tab, routes.LogPage.logs(), "list")
      ),
      section(inner)
    )

  def addUser(addForm: Form[NewUser], flash: Flash) =
    postableForm(routes.Accounts.formAddUser())(
      inGroup("username", Text, "Username"),
      passwordInputs(),
      blockSubmitButton("Add User"),
      addForm.globalError.fold(empty) { error =>
        alertDiv(AlertDanger, error.message)
      },
      flash.get(Accounts.Feedback).fold(empty) { feedback =>
        alertDiv(alertClass(flash), feedback)
      }
    )

  def alertClass(flash: Flash) =
    if (flash.get(Accounts.Success) contains "yes") AlertSuccess
    else AlertDanger

  def account(username: Username, passwordForm: Form[PasswordChange], flash: Flash) =
    indexMain("account", username)(
      headerRow(ColMd4)("Account"),
      rowColumn(ColMd4)(
        changePassword(username, passwordForm, flash)
      )
    )

  def changePassword(username: Username, passwordForm: Form[PasswordChange], flash: Flash) =
    postableForm(routes.Accounts.formChangePassword())(
      divClass(FormGroup)(
        label(`for` := "user")("Username"),
        divClass("controls")(
          span(`class` := s"$UneditableInput $InputMd", id := "user")(username.name)
        )
      ),
      passwordGroup("oldPassword", "Olad password"),
      passwordInputs("New password", "Repeat new password"),
      blockSubmitButton("Change Password"),
      passwordForm.globalError.orElse(passwordForm.errors.headOption).fold(empty) { error =>
        alertDiv(AlertDanger, error.message)
      },
      flash.get(Accounts.Feedback).fold(empty) { feedback =>
        alertDiv(AlertSuccess, feedback)
      }
    )

  def alertDiv(alertClass: String, message: String) =
    divClass(s"$Lead $alertClass", role := Alert)(message)

  def postableForm(onAction: Call, more: Modifier*) = form(role := FormRole, action := onAction, method := Post, more)

  def passwordInputs(firstLabel: String = "Password", repeatLabel: String = "Repeat password"): Modifier = Seq(
    passwordGroup("newPassword", firstLabel),
    passwordGroup("newPasswordAgain", repeatLabel)
  )

  def passwordGroup(elemId: String, labelText: String) =
    inGroup(elemId, Password, labelText)

  def inGroup(elemId: String, inType: String, labelText: String) =
    divClass(FormGroup)(
      label(`for` := elemId)(labelText),
      divClass("controls")(
        input(`type` := inType, id := elemId, name := elemId, `class` := s"$FormControl $InputMd", required)
      )
    )

  def blockSubmitButton(more: Modifier*) = button(`type` := Submit, `class` := s"$BtnPrimary $BtnBlock", more)

  def aboutBase(user: Username) = indexMain("about", user)(
    headerRow(ColMd6)("About"),
    rowColumn(ColMd8)(
      leadPara(s"MusicPimp ${BuildInfo.version}"),
      p("Developed by Michael Skogberg."),
      p("Check out ", aHref("https://www.musicpimp.org")("www.musicpimp.org"), " for the latest documentation.")
    ),
    rowColumn(ColMd8)(
      h2("Third Party Software"),
      p("This app uses the following third party software:"),
      div(`class` := "panel-group", id := "accordion")(
        licensePanel("collapseOne", Licenses.SCALA, "Scala, licensed under the ", "Scala License"),
        licensePanel("collapseTwo", Licenses.MIT, "software licensed under the ", "MIT License"),
        licensePanel("collapseThree", Licenses.APACHE, "software licensed under ", "Apache License 2.0"),
        licensePanel("collapseFour", Licenses.LGPL, "Tritonus plugins, licensed under the ", "GNU Lesser General Public License (LGPL)")
      ),
      p("... and icons by ", aHref("https://glyphicons.com")("Glyphicons"), ".")
    )
  )

  def licensePanel(elemId: String, licenseText: String, prefix: String, linkText: String) =
    thirdPartyPanel(elemId, licenseText)(
      panelSummary(prefix, elemId, linkText)
    )

  def thirdPartyPanel(elemId: String, innerContent: String)(toggleHtml: Modifier*) =
    divClass("panel-group", id := "accordion")(
      divClass("panel panel-default")(
        divClass("panel-heading")(
          spanClass("accordion-toggle")(toggleHtml)
        ),
        divClass(s"accordion-body $Collapse", id := elemId)(
          divClass("accordion-inner")(
            pre(`class` := "pre-scrollable")(innerContent)
          )
        )
      )
    )

  def panelSummary(prefix: String, elemId: String, linkText: String) =
    aHref(s"#$elemId", dataToggle := Collapse, dataParent := "#accordion")(linkText)

  def indexMain(tabName: String, user: Username, extraHeader: Modifier*)(inner: Modifier*) = {
    def navItem(thisTabName: String, url: Call, glyphiconName: String): TypedTag[String] = {
      glyphNavItem(thisTabName, thisTabName.toLowerCase, tabName, url, glyphiconName)
    }

    basePage("MusicPimp", extraHeader)(
      divClass(s"$Navbar $NavbarDefault $NavbarStaticTop")(
        divContainer(
          divClass(NavbarHeader)(
            hamburgerButton,
            aHref(routes.LibraryController.rootLibrary(), `class` := NavbarBrand)("MusicPimp")
          ),
          divClass(s"$NavbarCollapse $Collapse")(
            ulClass(s"$Nav $NavbarNav")(
              navItem("Library", routes.LibraryController.rootLibrary(), "list"),
              navItem("Player", routes.Website.player(), "music")
            ),
            ulClass(s"$Nav $NavbarNav $NavbarRight")(
              li(`class` := Dropdown)(
                aHref("#", `class` := DropdownToggle, dataToggle := Dropdown, role := Button, attr("aria-haspopup") := "true", attr("aria-expanded") := "false")(
                  glyphIcon("user"), s" ${user.name}", spanClass(Caret)
                ),
                ulClass(DropdownMenu)(
                  navItem("Account", routes.Accounts.account(), "pencil"),
                  navItem("Manage", routes.SettingsController.manage(), "wrench"),
                  navItem("About", routes.Website.about(), "globe"),
                  li(role := Separator, `class` := "divider"),
                  li(aHref(routes.Accounts.logout())(glyphIcon("off"), " Sign Out"))
                )
              )
            ),
            divClass(s"$ColMd2 $PullRight")(
              eye("okstatus", "eye-open green"),
              eye("failstatus", "eye-closed red")
            ),
            divClass(s"$ColMd4 $PullRight")(
              searchForm(None, formClass = NavbarForm, "")
            )
          )
        )
      )
    )
  }

  def saveButton(buttonText: String = "Save") =
    divClass(FormGroup)(
      divClass(s"$ColSmOffset2 $ColSm10")(
        button(`type` := Submit, `class` := BtnDefault)(buttonText)
      )
    )

  def glyphNavItem(thisTabName: String, thisTabId: String, activeTab: String, url: Call, glyphiconName: String): TypedTag[String] =
    iconNavItem(thisTabName, thisTabId, activeTab, url, glyphClass(glyphiconName))

  def iconNavItem(thisTabName: String, thisTabId: String, activeTab: String, url: Call, iconClass: String): TypedTag[String] = {
    val maybeActive = if (thisTabId == activeTab) Option(`class` := "active") else None
    li(maybeActive)(aHref(url)(iClass(iconClass), s" $thisTabName"))
  }

  def eye(elemId: String, glyphSuffix: String) =
    p(`class` := s"$NavbarText $PullRight $HiddenXs hide", id := elemId)(glyphIcon(glyphSuffix))

  def searchForm(query: Option[String] = None, formClass: String, size: String = InputGroupLg) =
    form(action := routes.SearchPage.search(), role := Search, `class` := formClass)(
      divClass(s"$InputGroup $size")(
        input(`type` := Text, `class` := FormControl, placeholder := query.getOrElse("artist, album or track..."), name := "term", id := "term"),
        divClass(InputGroupBtn)(
          button(`class` := BtnDefault, `type` := Submit)(glyphIcon("search"))
        )
      )
    )

  def basePage(title: String, extraHeader: Modifier*)(inner: Modifier*) = TagPage(
    html(lang := En)(
      head(
        titleTag(title),
        meta(name := "viewport", content := "width=device-width, initial-scale=1.0"),
        cssLink("//netdna.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"),
        cssLink("//maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css"),
        cssLink("//ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/themes/smoothness/jquery-ui.css"),
        cssLink(at("css/custom.css")),
        cssLink(at("css/footer.css")),
        extraHeader,
        jsScript("//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"),
        jsScript("//ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/jquery-ui.min.js"),
        jsScript("//netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js")
      ),
      body(
        div(id := "wrap")(
          inner,
          scripts,
          div(id := "push")(
          )
        ),
        div(id := "footer")(
          divContainer(
            p(
              spanClass(s"$TextMuted credit $PullRight")("Developed by ", aHref("https://github.com/malliina")("Michael Skogberg"), ".")
            )
          )
        )
      )
    )
  )
}
