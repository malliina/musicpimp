package com.malliina.musicpimp.tags

import ch.qos.logback.classic.Level
import com.malliina.musicpimp.BuildInfo
import com.malliina.musicpimp.db.DataTrack
import com.malliina.musicpimp.library.MusicFolder
import com.malliina.musicpimp.models._
import com.malliina.musicpimp.scheduler.ClockPlayback
import com.malliina.musicpimp.stats.{PopularEntry, RecentEntry, TopEntry}
import com.malliina.musicpimp.tags.PimpHtml._
import com.malliina.play.controllers.AccountForms
import com.malliina.play.models.Username
import com.malliina.play.tags.All._
import com.malliina.play.tags.TagPage
import controllers.musicpimp.{Accounts, SettingsController, UserFeedback, routes, Search => _}
import controllers.routes.Assets.at
import play.api.data.{Field, Form}
import play.api.i18n.Messages
import play.api.mvc.{Call, Flash}

import scalatags.Text.TypedTag
import scalatags.Text.all._

object PimpHtml {
  val FormSignin = "form-signin"
  val False = "false"
  val True = "true"
  val Hide = "hide"
  val WideContent = "wide-content"

  val dataIdAttr = attr("data-id")

  def forApp(isProd: Boolean): PimpHtml = {
    val scripts = ScalaScripts.forApp(BuildInfo.frontName, isProd)
    withJs(scripts.optimized, scripts.launcher)
  }

  def withJs(jsFiles: String*): PimpHtml =
    new PimpHtml(jsFiles.map(file => jsScript(at(file))): _*)

  def postableForm(onAction: Call, more: Modifier*) =
    form(role := FormRole, action := onAction, method := Post, more)

  def feedbackDiv(feedback: UserFeedback): TypedTag[String] = {
    val message = feedback.message
    if (feedback.isError) alertDanger(message)
    else alertSuccess(message)
  }

  def stripedHoverTable(headers: Seq[Modifier])(tableBody: Modifier*) =
    headeredTable(TableStripedHover, headers)(tableBody)

  def textInputBase(inType: String,
                    idAndName: String,
                    placeHolder: Option[String],
                    more: Modifier*) = {
    val placeholderAttr = placeHolder.fold(empty)(placeholder := _)
    namedInput(idAndName, `type` := inType, placeholderAttr, more)
  }
}

class PimpHtml(scripts: Modifier*) {
  def playlist(playlist: SavedPlaylist, username: Username) =
    manage("playlist", username)(
      PlaylistsHtml.playlistContent(playlist)
    )

  def playlists(lists: Seq[SavedPlaylist], username: Username) =
    manage("playlists", username)(
      PlaylistsHtml.playlistsContent(lists)
    )

  def users(us: Seq[Username],
            username: Username,
            listFeedback: Option[UserFeedback],
            addFeedback: Option[UserFeedback]) =
    manage("users", username)(
      UsersHtml.usersContent(us, listFeedback, addFeedback)
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
            Seq(
              td(track.title),
              td(track.artist),
              td(track.album),
              td(LibraryHtml.trackActions(track.id, Option("flex"))())
            )
          }
        } else {
          query.fold(empty) { term =>
            h3(s"No results for '$term'.")
          }
        }
      )
    )

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
            td(LibraryHtml.trackActions(entry.track.id, Option("flex"))())
          )
        }
      )
    )

  def logs(levelField: Field,
           levels: Seq[Level],
           currentLevel: Level,
           username: Username,
           errorMsg: Option[String]) =
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
      LoginHtml.loginContent(accounts, motd, formFeedback, topFeedback)
    )

  def flexLibrary(items: MusicFolder, username: Username) = {
    libraryBase("folders", WideContent, username)(
      LibraryHtml.libraryContent(items)
    )
  }

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
    manage("cloud", username)(
      CloudHtml.cloudContent
    )
  }

  def basePlayer(feedback: Option[UserFeedback], username: Username) =
    indexMain("player", username, Seq(cssLink(at("css/player.css"))))(
      PlayerHtml.playerContent(feedback)
    )

  def alarms(clocks: Seq[ClockPlayback], username: Username) =
    manage("alarms", username)(
      AlarmsHtml.alarmsContent(clocks)
    )

  def alarmEditor(form: Form[ClockPlayback],
                  feedback: Option[UserFeedback],
                  username: Username,
                  m: Messages) =
    manage("alarms", username)(
      AlarmsHtml.alarmEditorContent(form, feedback, m)
    )

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

  def alertClass(flash: Flash) =
    if (flash.get(Accounts.Success) contains "yes") AlertSuccess
    else AlertDanger

  def account(username: Username, feedback: Option[UserFeedback]) =
    indexMain("account", username)(
      UsersHtml.accountContent(username, feedback)
    )

  def aboutBase(user: Username) = indexMain("about", user)(
    AboutHtml.aboutBaseContent
  )

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
