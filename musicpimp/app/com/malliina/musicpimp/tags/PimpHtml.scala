package com.malliina.musicpimp.tags

import ch.qos.logback.classic.Level
import com.malliina.musicpimp.BuildInfo
import com.malliina.musicpimp.audio.{FolderMeta, TrackMeta}
import com.malliina.musicpimp.db.DataTrack
import com.malliina.musicpimp.library.{MusicFolder, PlaylistSubmission}
import com.malliina.musicpimp.models._
import com.malliina.musicpimp.scheduler.web.SchedulerStrings._
import com.malliina.musicpimp.scheduler.{ClockPlayback, WeekDay}
import com.malliina.musicpimp.stats.{PopularEntry, RecentEntry, TopEntry}
import com.malliina.musicpimp.tags.PlayBootstrap.helpSpan
import com.malliina.play.controllers.AccountForms
import com.malliina.play.models.Username
import com.malliina.play.tags.All._
import com.malliina.play.tags.TagPage
import controllers.musicpimp.{Accounts, Cloud, SettingsController, UserFeedback, routes, Search => _}
import controllers.routes.Assets.at
import play.api.data.{Field, Form}
import play.api.i18n.Messages
import play.api.mvc.{Call, Flash}

import scalatags.Text.TypedTag
import scalatags.Text.all._

object PimpHtml {
  def forApp(isProd: Boolean): PimpHtml = {
    val scripts = ScalaScripts.forApp(BuildInfo.frontName, isProd)
    withJs(scripts.optimized, scripts.launcher)
  }

  def withJs(jsFiles: String*): PimpHtml =
    new PimpHtml(jsFiles.map(file => jsScript(at(file))): _*)
}

class PimpHtml(scripts: Modifier*) {
  val FormSignin = "form-signin"
  val False = "false"
  val True = "true"
  val Hide = "hide"
  val WideContent = "wide-content"

  val dataIdAttr = attr("data-id")

  def playlist(playlist: SavedPlaylist, username: Username) =
    manage("playlist", username)(
      headerRow()("Playlist"),
      leadPara(playlist.name),
      tableView("This playlist is empty.", playlist.tracks, "Title", "Album", "Artist") { track =>
        Seq(td(track.title), td(track.album), td(track.artist))
      }
    )

  def playlists(lists: Seq[SavedPlaylist], username: Username) =
    manage("playlists", username)(
      headerRow()("Playlists"),
      tableView("No saved playlists.", lists, "Name", "Tracks", "Actions") { list =>
        Seq(
          td(aHref(routes.Playlists.playlist(list.id))(list.name)),
          td(list.tracks.size),
          td("Add/Edit/Delete")
        )
      }
    )

  def tableView[T](emptyText: String, items: Seq[T], headers: String*)(cells: T => Seq[Modifier]) =
    fullRow(
      if (items.isEmpty) {
        leadPara(emptyText)
      } else {
        responsiveTable(items)(headers: _*)(cells)
      }
    )

  def users(us: Seq[Username],
            username: Username,
            listFeedback: Option[UserFeedback],
            addFeedback: Option[UserFeedback]) =
    manage("users", username)(
      row(
        div6(
          headerDiv(
            h1("Users")
          ),
          stripedHoverTable(Seq("Username", "Actions"))(
            tbody(
              us map { u =>
                tr(
                  td(u.name),
                  td(postableForm(routes.Accounts.delete(u))(button(`class` := s"$BtnDanger $BtnXs")(" Delete")))
                )
              }
            )
          ),
          listFeedback.fold(empty)(feedbackDiv)
        ),
        div4(
          headerDiv(
            h1("Add user")
          ),
          addUser(addFeedback)
        )
      )
    )

  def search(query: Option[String], results: Seq[DataTrack], username: Username) =
    libraryBase("search", username)(
      headerRow()("Search"),
      row(
        div4(
          searchForm(None, "")
        ),
        divClass(s"$ColMd4 $ColMdOffset4")(
          button(`type` := Button, `class` := s"$BtnDefault $BtnLg", id := "refresh-button")(glyphIcon("refresh"), " "),
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

  def player(feedback: Option[String], username: Username) =
    basePlayer(feedback, username)

  def musicFolders(folders: Seq[String],
                   folderPlaceholder: String,
                   username: Username,
                   feedback: Option[UserFeedback]) =
    manage("folders", username)(
      headerRow(ColMd8)("Music Folders"),
      editFolders(folders, folderPlaceholder, feedback)
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
    libraryBase(tab, username)(
      headerRow()(s"$headerText ", small(`class` := HiddenXs)(s"by ${username.name}")),
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
    manage("logs", WideContent, username)(
      headerRow()("Logs"),
      errorMsg.fold(empty)(msg => fullRow(leadPara(msg))),
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

  def login(accounts: AccountForms,
            motd: Option[String],
            formFeedback: Option[UserFeedback],
            topFeedback: Option[UserFeedback]) =
    basePage("Welcome", cssLink(at("css/login.css")))(
      divContainer(
        rowColumn(s"$ColMd4 $FormSignin")(
          topFeedback.fold(empty)(feedbackDiv)
        ),
        rowColumn(ColMd4)(
          postableForm(routes.Accounts.formAuthenticate(), `class` := FormSignin, name := "loginForm")(
            h2("Please sign in"),
            formGroup(
              textInputBase(Text, accounts.userFormKey, Option("Username"), `class` := FormControl, autofocus)
            ),
            formGroup(
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
          formFeedback.fold(empty) { fb =>
            alertDiv(s"$AlertDanger $FormSignin", fb.message)
          }
        ),
        rowColumn(s"$ColMd4 $FormSignin")(
          motd.fold(empty)(message => p(message))
        )
      )
    )

  def library(relativePath: PimpPath,
              col1: MusicColumn,
              col2: MusicColumn,
              col3: MusicColumn,
              username: Username) =
    libraryBase("folders", username)(
      headerDiv(
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

  def flexLibrary(items: MusicFolder, username: Username) = {
    val relativePath = items.folder.path
    libraryBase("folders", WideContent, username)(
      headerDiv(
        h1("Library ", small(relativePath.path))
      ),
      row(
        items.folders map { folder =>
          musicItemDiv(
            renderLibraryFolder(folder)
          )
        },
        items.tracks map { track =>
          musicItemDiv(
            titledTrackActions(track)
          )
        }
      ),
      if (items.isEmpty && relativePath.path.isEmpty) {
        leadPara("The library is empty. To get started, add music folders under ",
          aHref(routes.SettingsController.settings())("Music Folders"), ".")
      } else {
        empty
      }
    )
  }

  def musicItemDiv = divClass("music-item col-xs-12 col-sm-6 col-md-4 col-lg-3")

  def renderLibraryFolder(folder: FolderMeta): Modifier = Seq[Modifier](
    folderActions(folder.id),
    " ",
    aHref(routes.LibraryController.library(folder.id), `class` := s"$Lead folder-link")(folder.title)
  )

  def renderColumn(col: MusicColumn, onlyColumn: Boolean = false) =
    divClass(if (onlyColumn) ColMd10 else ColMd4)(
      ulClass(ListUnstyled)(
        col.folders.map(f => li(Seq[Modifier](folderActions(f.id), " ", aHref(routes.LibraryController.library(f.id))(f.title)))),
        col.tracks.map(t => liClass(Lead)(titledTrackActions(t)))
      )
    )

  def libraryBase(tab: String, username: Username, extraHeader: Modifier*)(inner: Modifier*): TagPage =
    libraryBase(tab, Container, username, extraHeader)(inner)

  def libraryBase(tab: String, contentClass: String, username: Username, extraHeader: Modifier*)(inner: Modifier*): TagPage =
    indexMain("library", username, None, extraHeader)(
      divContainer(
        ulClass(NavTabs)(
          iconNavItem("Folders", "folders", tab, routes.LibraryController.rootLibrary(), "fa fa-folder-open"),
          iconNavItem("Most Played", "popular", tab, routes.Website.popular(), "fa fa-list"),
          iconNavItem("Most Recent", "recent", tab, routes.Website.recent(), "fa fa-clock-o"),
          iconNavItem("Search", "search", tab, routes.SearchPage.search(), "fa fa-search")
        )
      ),
      section(divClass(contentClass)(inner))
    )

  def folderActions(folder: FolderID) =
    musicItemActions("folder", folder.id, Option("folder-buttons"), ariaLabel := "folder action")()

  def titledTrackActions(track: TrackMeta) =
    trackActions(track.id)(
      dataButton(s"$BtnDefault $BtnBlock track play track-title", track.id.id)(track.title)
    )

  def trackActions(track: TrackID, extraClass: Option[String] = Option("track-buttons"))(inner: Modifier*) =
    musicItemActions("track", track.id, extraClass)(inner)

  def musicItemActions(itemClazz: String, itemId: String, extraClass: Option[String], groupAttrs: Modifier*)(inner: Modifier*) = {
    val extra = extraClass.map(c => s" $c").getOrElse("")
    divClass(s"$BtnGroup$extra", role := Group, groupAttrs)(
      glyphButton(s"$BtnPrimary $itemClazz play", "play", itemId),
      glyphButton(s"$BtnDefault $itemClazz add", "plus", itemId),
      inner
    )
  }

  def glyphButton(clazz: String, glyph: String, buttonId: String) =
    dataButton(clazz, buttonId)(glyphIcon(glyph))

  def dataButton(clazz: String, buttonId: String) =
    button(`type` := Button, `class` := clazz, dataIdAttr := buttonId)

  def editFolders(folders: Seq[String],
                  folderPlaceholder: String,
                  errorMessage: Option[UserFeedback]) =
    halfRow(
      ulClass(ListUnstyled)(
        folders.map(renderFolder)
      ),
      postableForm(routes.SettingsController.newFolder(), `class` := FormHorizontal, name := "newFolderForm")(
        divClass(InputGroup)(
          spanClass(InputGroupAddon)(glyphIcon("folder-open")),
          textInputBase(Text, SettingsController.Path, Option(folderPlaceholder), `class` := FormControl, required),
          spanClass(InputGroupBtn)(
            submitButton(`class` := BtnPrimary)(glyphIcon("plus"), " Add")
          )
        ),
        errorMessage.fold(empty)(feedbackDiv)
      )
    )

  def renderFolder(folder: String) =
    postableForm(routes.SettingsController.deleteFolder(folder), `class` := FormHorizontal)(
      divClass(InputGroup)(
        spanClass(InputGroupAddon)(glyphIcon("folder-open")),
        spanClass(s"$UneditableInput $FormControl")(folder),
        spanClass(InputGroupBtn)(
          submitButton(`class` := BtnDanger)(glyphIcon("remove"), " Delete")
        )
      )
    )

  def cloud(cloudId: Option[CloudID],
            feedback: Option[UserFeedback],
            username: Username) = {
    val successFeedback = cloudId
      .map(id => s"Connected. You can now access this server using your credentials and this cloud ID: $id")
      .map(UserFeedback.success)
    val fb = feedback orElse successFeedback
    manage("cloud", username)(
      headerRow()("Cloud"),
      div(id := "cloud-form")(
        leadPara("Connecting...")
        //        halfRow(cloudForm(cloudId)),
        //        fb.fold(empty)(f => halfRow(feedbackDiv(f)))
      ),
      halfRow(
        p("How does this work?"),
        p("This server will open a connection to a machine on the internet. Your mobile device connects to the " +
          "same machine on the internet and communicates with this server through the machine both parties have " +
          "connected to. All traffic is encrypted. All music is streamed.")
      )
    )
  }

  def cloudForm(cloudId: Option[CloudID]) = {
    val title = cloudId.fold("Connect")(_ => "Disconnect")
    postableForm(routes.Cloud.toggle(), name := "toggleForm")(
      if (cloudId.isEmpty) {
        formGroup(
          labelFor(Cloud.idFormKey)("Desired cloud ID (optional)"),
          textInput(Text, FormControl, Cloud.idFormKey, Option("Your desired ID or leave empty"))
        )
      } else {
        empty
      },
      blockSubmitButton(id := "toggleButton")(title)
    )
  }

  def basePlayer(feedback: Option[String], username: Username, scripts: Modifier*) =
    indexMain("player", username, scripts ++ Seq(cssLink(at("css/player.css"))))(
      row(
        divClass(ColMd6)(
          headerDiv(h1("Player")),
          div(id := "playerDiv", style := "display: none")(
            feedback.fold(empty) { fb =>
              fullRow(
                pClass(s"$Lead $Alert", id := "feedback")(fb)
              )
            },
            fullRow(
              pClass(Lead, id := "notracktext")(
                "No track. Play one from the ",
                aHref(routes.LibraryController.rootLibrary())("library"),
                " or stream from a mobile device."
              )
            ),
            playerCtrl
          )
        ),
        divClass(ColMd6)(
          headerDiv(h1("Playlist")),
          pClass(Lead, id := "empty_playlist_text")("The playlist is empty."),
          ol(id := "playlist")
        )
      )
    )

  def playerControls = {
    divClass(ColMd6)(
      playerCtrl
    )
  }

  def playerCtrl: Modifier = {
    val centerAttr = `class` := "track-meta"
    Seq(
      fullRow(
        h2(id := "title", centerAttr)("No track"),
        h4(id := "album", centerAttr),
        h3(id := "artist", centerAttr)
      ),
      fullRow(
        divClass(ColMd11)(
          div(id := "slider")
        ),
        divClass(s"$Row $ColMd11", id := "progress")(
          span(id := "pos")("00:00"), " / ", span(id := "duration")("00:00")
        ),
        divClass(s"$Row $ColMd11 text-center")(
          imageInput(at("img/transport.rew.png"), id := "prevButton"),
          imageInput(at("img/light/transport.play.png"), id := "playButton"),
          imageInput(at("img/light/transport.pause.png"), id := "pauseButton", style := "display: none"),
          imageInput(at("img/light/transport.ff.png"), id := "nextButton")
        ),
        divClass(s"$Row $ColMd11 $VisibleLg")(
          divClass(ColMd3)(
            imageInput(at("img/light/appbar.sound.3.png"), id := "volumeButton", `class` := PullRight)
          ),
          divClass(ColMd8)(
            divClass(s"$ColMd12 $PullLeft", id := "volume")
          )
        )
      )
    )
  }

  def alarms(clocks: Seq[ClockPlayback], username: Username) =
    manage("alarms", username)(
      headerRow()("Alarms"),
      fullRow(
        stripedHoverTable(Seq("Description", "Enabled", "Actions"))(
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

  def stripedHoverTable(headers: Seq[Modifier])(tableBody: Modifier*) =
    headeredTable(TableStripedHover, headers)(tableBody)

  def alarmActions(id: String) =
    divClass(BtnGroup)(
      aHref(routes.Alarms.editAlarm(id), `class` := s"$BtnDefault $BtnSm")(glyphIcon("edit"), " Edit"),
      aHref("#", dataToggle := Dropdown, `class` := s"$BtnDefault $BtnSm $DropdownToggle")(spanClass(Caret)),
      ulClass(DropdownMenu)(
        jsListElem("delete", id, "remove", "Delete"),
        jsListElem("play", id, "play", "Play"),
        jsListElem("stop", id, "stop", "Stop")
      )
    )

  def jsListElem(clazz: String, dataId: String, glyph: String, linkText: String) =
    liHref("#", `class` := clazz, dataIdAttr := dataId)(glyphIcon(glyph), s" $linkText")

  def alarmEditor(form: Form[ClockPlayback], feedback: Option[UserFeedback], username: Username, m: Messages) =
    manage("alarms", username)(
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
          feedback.fold(empty)(fb => divClass(s"$Lead $ColSmOffset2")(feedbackDiv(fb)))
        )
      )
    )

  def checkField(field: Field, labelText: String) = {
    val checkedAttr = if (field.value.contains("on")) checked else empty
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

  def weekdayCheckboxes(field: Field, messages: Messages) = {
    val errorClass = if (field.hasErrors) s" $HasError" else ""
    divClass(s"$FormGroup$errorClass")(
      labelFor(field.id, `class` := s"$ColSm2 $ControlLabel")("Days"),
      divClass(ColSm4, id := field.id)(
        divClass(Checkbox)(
          label(
            input(`type` := Checkbox, value := "every", id := "every")("Every day")
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
        input(`type` := Checkbox,
          value := field.value.getOrElse(weekDay.shortName),
          id := weekDay.shortName,
          name := s"${field.name}[$index]",
          checkedAttr
        )
      )(s" ${weekDay.longName}")
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
    namedInput(idAndName, `type` := inType, placeholderAttr, more)
  }

  def manage(tab: String, username: Username, extraHeader: Modifier*)(inner: Modifier*): TagPage =
    manage(tab, Container, username, extraHeader: _*)(inner: _*)

  def manage(tab: String, contentClass: String, username: Username, extraHeader: Modifier*)(inner: Modifier*): TagPage =
    indexMain("manage", username, None, extraHeader)(
      divContainer(
        ulClass(NavTabs)(
          glyphNavItem("Music Folders", "folders", tab, routes.SettingsController.settings(), "folder-open"),
          glyphNavItem("Users", "users", tab, routes.Accounts.users(), "user"),
          glyphNavItem("Alarms", "alarms", tab, routes.Alarms.alarms(), "time"),
          glyphNavItem("Cloud", "cloud", tab, routes.Cloud.cloud(), "cloud"),
          glyphNavItem("Logs", "logs", tab, routes.LogPage.logs(), "list")
        )
      ),
      section(divClass(contentClass)(inner))
    )

  def addUser(addFeedback: Option[UserFeedback]) =
    postableForm(routes.Accounts.formAddUser())(
      inGroup("username", Text, "Username"),
      passwordInputs(),
      blockSubmitButton()("Add User"),
      addFeedback.fold(empty)(feedbackDiv)
    )

  def alertClass(flash: Flash) =
    if (flash.get(Accounts.Success) contains "yes") AlertSuccess
    else AlertDanger

  def account(username: Username, feedback: Option[UserFeedback]) =
    indexMain("account", username)(
      headerRow(ColMd4)("Account"),
      rowColumn(ColMd4)(
        changePassword(username, feedback)
      )
    )

  def changePassword(username: Username, feedback: Option[UserFeedback]) =
    postableForm(routes.Accounts.formChangePassword())(
      formGroup(
        labelFor("user")("Username"),
        divClass("controls")(
          spanClass(s"$UneditableInput $InputMd", id := "user")(username.name)
        )
      ),
      passwordGroup("oldPassword", "Old password"),
      passwordInputs("New password", "Repeat new password"),
      blockSubmitButton("Change Password"),
      feedback.fold(empty)(feedbackDiv)
    )

  def postableForm(onAction: Call, more: Modifier*) =
    form(role := FormRole, action := onAction, method := Post, more)

  def passwordInputs(firstLabel: String = "Password", repeatLabel: String = "Repeat password"): Modifier = Seq(
    passwordGroup("newPassword", firstLabel),
    passwordGroup("newPasswordAgain", repeatLabel)
  )

  def passwordGroup(elemId: String, labelText: String) =
    inGroup(elemId, Password, labelText)

  def inGroup(elemId: String, inType: String, labelText: String) =
    formGroup(
      labelFor(elemId)(labelText),
      divClass("controls")(
        namedInput(elemId, `type` := inType, `class` := s"$FormControl $InputMd", required)
      )
    )

  def aboutBase(user: Username) = indexMain("about", user)(
    headerRow(ColMd6)("About"),
    rowColumn(ColMd8)(
      leadPara(s"MusicPimp ${BuildInfo.version}"),
      p("Developed by Michael Skogberg."),
      p("Check out ", aHref("https://www.musicpimp.org")("www.musicpimp.org"), " for the latest documentation.")
    ),
    rowColumn(ColMd8)(
      h3("Third Party Software"),
      p("This app uses the following third party software:"),
      divClass("panel-group", id := "accordion")(
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
      prefix, panelSummary(prefix, elemId, linkText)
    )

  def thirdPartyPanel(elemId: String, innerContent: String)(toggleHtml: Modifier*) =
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

  def panelSummary(prefix: String, elemId: String, linkText: String) =
    aHref(s"#$elemId", dataToggle := Collapse, dataParent := "#accordion")(linkText)

  def indexMain(tabName: String,
                user: Username,
                extraHeader: Modifier*)(inner: Modifier*): TagPage =
    indexMain(tabName, user, Option(divContainer), extraHeader: _*)(inner: _*)

  def indexMain(tabName: String,
                user: Username,
                contentWrapper: Option[TypedTag[String]],
                extraHeader: Modifier*)(inner: Modifier*): TagPage = {
    def navItem(thisTabName: String, url: Call, glyphiconName: String): TypedTag[String] =
      glyphNavItem(thisTabName, thisTabName.toLowerCase, tabName, url, glyphiconName)

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
                aHref("#", `class` := DropdownToggle, dataToggle := Dropdown, role := Button, ariaHasPopup := True, ariaExpanded := False)(
                  glyphIcon("user"), s" ${user.name} ", spanClass(Caret)
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
              eye("failstatus", "eye-close red")
            ),
            divClass(s"$ColMd4 $PullRight")(
              searchForm(None, formClass = NavbarForm, "")
            )
          )
        )
      ),
      section(contentWrapper.map(wrapper => wrapper(inner)).getOrElse(inner))
    )
  }

  def saveButton(buttonText: String = "Save") =
    formGroup(
      divClass(s"$ColSmOffset2 $ColSm10")(
        defaultSubmitButton(buttonText)
      )
    )

  def glyphNavItem(thisTabName: String, thisTabId: String, activeTab: String, url: Call, glyphiconName: String): TypedTag[String] =
    iconNavItem(thisTabName, thisTabId, activeTab, url, glyphClass(glyphiconName))

  def iconNavItem(thisTabName: String, thisTabId: String, activeTab: String, url: Call, iconClass: String): TypedTag[String] = {
    val maybeActive = if (thisTabId == activeTab) Option(`class` := "active") else None
    li(maybeActive)(aHref(url)(iClass(iconClass), s" $thisTabName"))
  }

  def eye(elemId: String, glyphSuffix: String) =
    pClass(s"$NavbarText $PullRight $HiddenXs $Hide", id := elemId)(glyphIcon(glyphSuffix))

  def searchForm(query: Option[String] = None, formClass: String, size: String = InputGroupLg) =
    form(action := routes.SearchPage.search(), role := Search, `class` := formClass)(
      divClass(s"$InputGroup $size")(
        input(`type` := Text, `class` := FormControl, placeholder := query.getOrElse("artist, album or track..."), name := "term", id := "term"),
        divClass(InputGroupBtn)(
          defaultSubmitButton(glyphIcon("search"))
        )
      )
    )

  def feedbackDiv(feedback: UserFeedback): TypedTag[String] = {
    val message = feedback.message
    if (feedback.isError) alertDanger(message)
    else alertSuccess(message)
  }

  def basePage(title: String, extraHeader: Modifier*)(inner: Modifier*) = TagPage(
    html(lang := En)(
      head(
        meta(charset := "UTF-8"),
        titleTag(title),
        deviceWidthViewport,
        cssLink("//netdna.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"),
        cssLink("//maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css"),
        cssLink("//ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/themes/smoothness/jquery-ui.css"),
        cssLink(at("css/custom.css")),
        cssLink(at("css/footer.css")),
        jsScript("//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"),
        jsScript("//ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/jquery-ui.min.js"),
        jsScript("//netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"),
        extraHeader
      ),
      body(
        div(id := "wrap")(
          inner,
          scripts,
          div(id := "push")()
        ),
        div(id := "footer")(
          nav(`class` := s"$Navbar $NavbarDefault", id := "bottom-navbar")(
            divContainer(
              divClass(s"$Collapse $NavbarCollapse")(
                ulClass(s"$Nav $NavbarNav $HiddenXs")(
                  awesomeLi("footer-backward", "step-backward"),
                  awesomeLi("footer-play", "play"),
                  awesomeLi("footer-pause", "pause"),
                  awesomeLi("footer-forward", "step-forward")
                ),
                navbarPara("footer-progress"),
                navbarPara("footer-title"),
                navbarPara("footer-artist"),
                div(`class` := s"$Nav $NavbarNav $NavbarRight", id := "footer-credit")(
                  p(
                    spanClass(s"$TextMuted $NavbarText $PullRight")("Developed by ", aHref("https://github.com/malliina")("Michael Skogberg"), ".")
                  )
                )
              )
            )
          )
        )
      )
    )
  )

  def navbarPara(elemId: String) =
    pClass(NavbarText, id := elemId)("")

  def awesomeLi(elemId: String, faIcon: String) =
    li(id := elemId, `class` := Hide)(aHref("#")(iClass(s"fa fa-$faIcon")))

}
