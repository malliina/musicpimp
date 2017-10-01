package com.malliina.musicpimp.html

import ch.qos.logback.classic.Level
import com.malliina.musicpimp.BuildInfo
import com.malliina.musicpimp.assets.AppAssets
import com.malliina.musicpimp.db.DataTrack
import com.malliina.musicpimp.js.{FooterStrings, FrontStrings}
import com.malliina.musicpimp.library.MusicFolder
import com.malliina.musicpimp.models._
import com.malliina.musicpimp.scheduler.ClockPlayback
import com.malliina.musicpimp.stats._
import com.malliina.musicpimp.html.PimpHtml._
import com.malliina.musicpimp.html.PimpHtml.reverseAssets.versioned
import com.malliina.play.controllers.AccountForms
import com.malliina.play.models.Username
import com.malliina.play.tags.All._
import com.malliina.play.tags.TagPage
import controllers.Assets.Asset
import controllers.ReverseAssets
import controllers.musicpimp.{UserFeedback, routes}
import play.api.data.{Field, Form}
import play.api.i18n.Messages
import play.api.mvc.Call

import scalatags.Text.TypedTag
import scalatags.Text.all._

object PimpHtml {
  val FormSignin = "form-signin"
  val False = "false"
  val True = "true"
  val WideContent = "wide-content"
  val HiddenSmall = "hidden-xs hidden-sm"

  val dataIdAttr = attr("data-id")

  val reverseAssets = new ReverseAssets("")

  def at(file: String): Call = versioned(file)

  def versioned(file: Asset) = reverseAssets.versioned(file)

  def forApp(isProd: Boolean): PimpHtml = {
    val scripts = ScalaScripts.forApp(BuildInfo.frontName, isProd)
    withJs(scripts.optimized)
  }

  def withJs(jsFiles: String*): PimpHtml =
    new PimpHtml(jsFiles.map(file => jsScript(versioned(file))): _*)

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

class PimpHtml(scripts: Modifier*) extends FooterStrings with FrontStrings {
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
      SearchHtml.searchContent(query, results)
    )

  def musicFolders(folders: Seq[String],
                   folderPlaceholder: String,
                   username: Username,
                   feedback: Option[UserFeedback]) =
    manage("folders", username)(
      headerRow(ColMd8)("Music Folders"),
      SettingsHtml.editFolders(folders, folderPlaceholder, feedback)
    )

  def mostRecent(result: RecentList) =
    topList("recent", result.username, TopHtml.mostRecentContent(result))

  def mostPopular(result: PopularList) =
    topList("popular", result.username, TopHtml.mostPopular(result))

  def topList(tab: String, username: Username, inner: Modifier) =
    libraryBase(tab, username)(inner)

  def logs(levelField: Field,
           levels: Seq[Level],
           currentLevel: Level,
           username: Username,
           feedback: Option[UserFeedback]) =
    manage("logs", WideContent, username)(
      LogsHtml.logsContent(levelField, levels, currentLevel, feedback)
    )

  def login(conf: LoginConf) =
    basePage("Welcome")(
      LoginHtml.loginContent(conf)
    )

  def flexLibrary(items: MusicFolder, username: Username) = {
    libraryBase("folders", WideContent, username)(
      LibraryHtml.libraryContent(items)
    )
  }

  def libraryBase(tab: String, username: Username)(inner: Modifier*): TagPage =
    libraryBase(tab, Container, username)(inner)

  def libraryBase(tab: String, contentClass: String, username: Username)(inner: Modifier*): TagPage =
    indexMain("library", username, None)(
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

  def cloud(cloudId: Option[CloudID],
            feedback: Option[UserFeedback],
            username: Username) = {
    manage("cloud", username)(
      CloudHtml.cloudContent
    )
  }

  def basePlayer(feedback: Option[UserFeedback], username: Username) =
    indexMain("player", username)(
      PlayerHtml.playerContent(feedback)
    )

  def alarms(clocks: Seq[ClockPlayback], username: Username) =
    manage("alarms", username)(
      AlarmsHtml.alarmsContent(clocks)
    )

  def alarmEditor(conf: AlarmConf) =
    manage("alarms", conf.username)(
      AlarmsHtml.alarmEditorContent(conf)
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
              eye(OkStatus, "eye-open green"),
              eye(FailStatus, "eye-close red")
            ),
            divClass(s"$ColMd4 $PullRight $HiddenSmall")(
              SearchHtml.searchForm(None, formClass = NavbarForm, "")
            )
          )
        )
      ),
      section(contentWrapper.map(wrapper => wrapper(inner)).getOrElse(inner))
    )
  }

  def glyphNavItem(thisTabName: String,
                   thisTabId: String,
                   activeTab: String,
                   url: Call,
                   glyphiconName: String): TypedTag[String] =
    iconNavItem(thisTabName, thisTabId, activeTab, url, glyphClass(glyphiconName))

  def iconNavItem(thisTabName: String,
                  thisTabId: String,
                  activeTab: String,
                  url: Call,
                  iconClass: String): TypedTag[String] = {
    val maybeActive =
      if (thisTabId == activeTab) Option(`class` := "active")
      else None
    li(maybeActive)(aHref(url)(iClass(iconClass), s" $thisTabName"))
  }

  def eye(elemId: String, glyphSuffix: String) =
    pClass(s"$NavbarText $PullRight $HiddenXs $HiddenClass", id := elemId)(glyphIcon(glyphSuffix))

  def basePage(title: String, extraHeader: Modifier*)(inner: Modifier*) = TagPage(
    html(lang := En)(
      head(
        meta(charset := "UTF-8"),
        titleTag(title),
        deviceWidthViewport,
        cssLink("//netdna.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"),
        cssLink("//maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css"),
        cssLink("//ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/themes/smoothness/jquery-ui.css"),
        cssLink(at("css/main.css")),
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
          nav(`class` := s"$Navbar $NavbarDefault", id := BottomNavbar)(
            divContainer(
              divClass(s"$Collapse $NavbarCollapse")(
                ulClass(s"$Nav $NavbarNav $HiddenXs")(
                  awesomeLi(FooterBackward, "step-backward"),
                  awesomeLi(FooterPlay, "play"),
                  awesomeLi(FooterPause, "pause"),
                  awesomeLi(FooterForward, "step-forward")
                ),
                navbarPara(FooterProgress),
                navbarPara(FooterTitle),
                navbarPara(FooterArtist),
                div(`class` := s"$Nav $NavbarNav $NavbarRight", id := FooterCredit)(
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
    li(id := elemId, `class` := HiddenClass)(aHref("#")(iClass(s"fa fa-$faIcon")))
}
