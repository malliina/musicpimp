package com.malliina.musicpimp.html

import ch.qos.logback.classic.Level
import com.malliina.html.{Bootstrap, HtmlTags}
import com.malliina.musicpimp.BuildInfo
import com.malliina.musicpimp.db.DataTrack
import com.malliina.musicpimp.html.PimpBootstrap._
import com.malliina.musicpimp.html.PimpBootstrap.tags._
import com.malliina.musicpimp.html.PimpHtml._
import com.malliina.musicpimp.js.{FooterStrings, FrontStrings}
import com.malliina.musicpimp.library.MusicFolder
import com.malliina.musicpimp.messaging.TokenInfo
import com.malliina.musicpimp.models._
import com.malliina.musicpimp.scheduler.FullClockPlayback
import com.malliina.musicpimp.stats._
import com.malliina.play.tags.PlayTags.callAttr
import com.malliina.play.tags.TagPage
import com.malliina.values.Username
import controllers.Assets.Asset
import controllers.ReverseAssets
import controllers.musicpimp.{UserFeedback, routes}
import play.api.data.Field
import play.api.mvc.Call
import scalatags.Text.TypedTag
import scalatags.Text.all._

object PimpHtml {
  val FormSignin = "form-signin"
  val False = "false"
  val True = "true"
  val WideContent = "wide-content"
  val HiddenSmall = "d-none d-sm-block"

  val dataIdAttr = data("id")

  val reverseAssets = new ReverseAssets("")

  def at(file: String): Call = versioned(file)

  def versioned(file: Asset): Call = reverseAssets.versioned(file)

  def forApp(isProd: Boolean): PimpHtml = {
    val scripts = ScalaScripts.forApp(BuildInfo.frontName, isProd)
    withJs(scripts)
  }

  def withJs(jsFiles: ScalaScripts): PimpHtml =
    new PimpHtml(jsFiles)

  def postableForm(onAction: Call, more: Modifier*) =
    form(role := FormRole, action := onAction, method := Post, more)

  def feedbackDiv(feedback: UserFeedback) = {
    val message = feedback.message
    if (feedback.isError) alertDanger(message)
    else alertSuccess(message)
  }

  def stripedHoverTableSmall(headers: Seq[Modifier])(tableBody: Modifier*) =
    stripedTable(tables.defaultClass, headers)(tableBody)

  def stripedHoverTable(headers: Seq[Modifier])(tableBody: Modifier*) =
    stripedTable(tables.stripedHover, headers)(tableBody)

  def stripedTable(tableClass: String, headers: Seq[Modifier])(tableBody: Modifier*) =
    headeredTable(tableClass, headers)(tableBody)

  def textInputBase(
    inType: String,
    idAndName: String,
    placeHolder: Option[String],
    more: Modifier*
  ) = {
    val placeholderAttr = placeHolder.fold(empty)(placeholder := _)
    namedInput(idAndName, `type` := inType, placeholderAttr, more)
  }
}

class PimpHtml(scripts: ScalaScripts)
  extends Bootstrap(HtmlTags)
  with FooterStrings
  with FrontStrings {
  def playlist(playlist: SavedPlaylist, username: Username) =
    manage("playlist", username)(
      PlaylistsHtml.playlistContent(playlist)
    )

  def playlists(lists: Seq[SavedPlaylist], username: Username) =
    manage("playlists", username)(
      PlaylistsHtml.playlistsContent(lists)
    )

  def users(content: UsersContent) =
    manage("users", content.username)(
      UsersHtml.usersContent(content)
    )

  def search(query: Option[String], results: Seq[DataTrack], username: Username) =
    libraryBase("search", username)(
      SearchHtml.searchContent(query, results)
    )

  def musicFolders(content: LibraryContent) =
    manage("folders", content.username)(
      headerRow("Music Folders", col.md.eight),
      SettingsHtml.editFolders(content)
    )

  def mostRecent(result: RecentList) =
    topList("recent", result.username, TopHtml.mostRecentContent(result))

  def mostPopular(result: PopularList) =
    topList("popular", result.username, TopHtml.mostPopular(result))

  def topList(tab: String, username: Username, inner: Modifier) =
    libraryBase(tab, username)(inner)

  def logs(
    levelField: Field,
    levels: Seq[Level],
    currentLevel: Level,
    username: Username,
    feedback: Option[UserFeedback]
  ) =
    manage("logs", WideContent, username)(
      LogsHtml.logsContent(levelField, levels, currentLevel, feedback)
    )

  def login(conf: LoginContent) =
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

  def libraryBase(tab: String, contentClass: String, username: Username)(
    inner: Modifier*
  ): TagPage =
    indexMain("library", username, None)(
      divContainer(
        ulClass(NavTabs)(
          iconNavItem(
            "Folders",
            "folders",
            tab,
            routes.LibraryController.rootLibrary(),
            iClass("fa fa-folder-open")
          ),
          iconNavItem(
            "Most Played",
            "popular",
            tab,
            routes.Website.popular(),
            iClass("fa fa-list")
          ),
          iconNavItem(
            "Most Recent",
            "recent",
            tab,
            routes.Website.recent(),
            iClass("fa fa-clock-o")
          ),
          iconNavItem("Search", "search", tab, routes.SearchPage.search(), iClass("fa fa-search"))
        )
      ),
      section(divClass(contentClass)(inner))
    )

  def cloud(cloudId: Option[CloudID], feedback: Option[UserFeedback], username: Username) =
    manage("cloud", username)(
      CloudHtml.cloudContent
    )

  def basePlayer(feedback: Option[UserFeedback], username: Username) =
    indexMain("player", username)(
      PlayerHtml.playerContent(feedback)
    )

  def alarms(clocks: Seq[FullClockPlayback], username: Username) =
    manage("alarms", username)(
      AlarmsHtml.alarmsContent(clocks)
    )

  def tokens(tokens: Seq[TokenInfo], username: Username, feedback: Option[UserFeedback]) =
    manage("tokens", username)(
      AlarmsHtml.tokens(tokens, feedback)
    )

  def alarmEditor(conf: AlarmContent) =
    manage("alarms", conf.username)(
      AlarmsHtml.alarmEditorContent(conf)
    )

  def manage(tab: String, username: Username)(inner: Modifier*): TagPage =
    manage(tab, Container, username)(inner: _*)

  def manage(tab: String, contentClass: String, username: Username)(inner: Modifier*): TagPage = {
    def fa(icon: String) = iClass(s"fa fa-$icon")

    indexMain("manage", username, None)(
      divContainer(
        ulClass(NavTabs)(
          iconNavItem(
            "Music Folders",
            "folders",
            tab,
            routes.SettingsController.settings(),
            fa("folder-open")
          ),
          iconNavItem("Users", "users", tab, routes.Accounts.users(), fa("user")),
          iconNavItem("Alarms", "alarms", tab, routes.Alarms.alarms(), fa("clock")),
          iconNavItem("Tokens", "tokens", tab, routes.Alarms.tokens(), fa("key")),
          iconNavItem("Cloud", "cloud", tab, routes.Cloud.cloud(), fa("cloud")),
          iconNavItem("Logs", "logs", tab, routes.LogPage.logs(), fa("list"))
        )
      ),
      section(divClass(contentClass)(inner))
    )
  }

  def account(username: Username, feedback: Option[UserFeedback]) =
    indexMain("account", username)(
      UsersHtml.accountContent(username, feedback)
    )

  def aboutBase(user: Username) = indexMain("about", user)(
    AboutHtml.aboutBaseContent
  )

  def indexMain(tabName: String, user: Username)(inner: Modifier*): TagPage =
    indexMain(tabName, user, Option(div(`class` := Container)))(inner: _*)

  def indexMain(
    tabName: String,
    user: Username,
    contentWrapper: Option[TypedTag[String]]
  )(inner: Modifier*): TagPage = {
    def navItem(thisTabName: String, url: Call, iconicName: String): TypedTag[String] =
      iconicNavItem(thisTabName, thisTabName.toLowerCase, tabName, url, iconicName)

    basePage("MusicPimp")(
      navbar.basic(
        routes.LibraryController.rootLibrary(),
        "MusicPimp",
        modifier(
          ulClass(s"${navbars.Nav} $MrAuto")(
            navItem("Library", routes.LibraryController.rootLibrary(), "list"),
            navItem("Player", routes.Website.player(), "musical-note")
          ),
          ulClass(s"$Nav ${navbars.Nav} ${navbars.Right}")(
            divClass(s"$HiddenSmall")(
              SearchHtml.navbarSearch()
            ),
            li(`class` := s"nav-item $Dropdown")(
              a(
                href := "#",
                `class` := s"nav-link $DropdownToggle",
                dataToggle := Dropdown,
                role := Button,
                aria.haspopup := tags.True,
                aria.expanded := tags.False
              )(
                iconic("person"),
                s" ${user.name} ",
                spanClass(Caret)
              ),
              ulClass(DropdownMenu)(
                navItem("Account", routes.Accounts.account(), "pencil"),
                navItem("Manage", routes.SettingsController.manage(), "wrench"),
                navItem("About", routes.Website.about(), "globe"),
                divClass("dropdown-divider"),
                li(
                  a(href := routes.Accounts.logout(), `class` := "nav-link")(
                    iconic("account-logout"),
                    " Sign Out"
                  )
                )
              )
            ),
            div(
              eye(OkStatus, "bolt green"),
              eye(FailStatus, "bolt red")
            )
          )
        )
      ),
      section(contentWrapper.map(wrapper => wrapper(inner)).getOrElse(inner))
    )
  }

  def iconicNavItem(
    thisTabName: String,
    thisTabId: String,
    activeTab: String,
    url: Call,
    iconicName: String
  ): TypedTag[String] =
    iconNavItem(thisTabName, thisTabId, activeTab, url, iconic(iconicName))

  def iconNavItem(
    thisTabName: String,
    thisTabId: String,
    activeTab: String,
    url: Call,
    iconHtml: Modifier
  ): TypedTag[String] = {
    val linkClass =
      if (thisTabId == activeTab) "nav-link active"
      else "nav-link"
    li(`class` := "nav-item")(a(href := url, `class` := linkClass)(iconHtml, s" $thisTabName"))
  }

  def eye(elemId: String, iconicName: String) =
    span(`class` := s"${navbars.Text} $HiddenClass", id := elemId)(iconic(iconicName))

  def basePage(title: String, extraHeader: Modifier*)(inner: Modifier*) = TagPage(
    html(lang := En)(
      head(
        meta(charset := "UTF-8"),
        titleTag(title),
        deviceWidthViewport,
        cssLinkHashed(
          "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css",
          "sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm"
        ),
        cssLink("https://use.fontawesome.com/releases/v5.0.6/css/all.css"),
        cssLink("//maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css"),
        cssLink("https://code.jquery.com/ui/1.12.1/themes/base/jquery-ui.css"),
        cssLink(at("styles.css")),
        extraHeader
      ),
      body(
        inner,
        scripts.jsFiles.map(file => jsScript(versioned(file))),
        footer(`class` := "footer", id := FooterId)(
          nav(
            `class` := s"${navbars.Navbar} navbar-expand-sm ${navbars.Light} ${navbars.BgLight} $HiddenSmall",
            id := BottomNavbar
          )(
            divContainer(
              ulClass(s"${navbars.Nav}")(
                footerIcon(FooterBackward, "step-backward"),
                footerIcon(FooterPlay, "play"),
                footerIcon(FooterPause, "pause"),
                footerIcon(FooterForward, "step-forward"),
                navbarPara(FooterProgress),
                navbarPara(FooterTitle),
                navbarPara(FooterArtist)
              ),
              div(`class` := s"${navbars.Nav} ${navbars.Right}", id := FooterCredit)(
                spanClass(s"${text.muted} ${navbars.Text} float-right")(
                  "Developed by ",
                  a(href := "https://malliina.com")("Michael Skogberg"),
                  "."
                )
              )
            )
          )
        )
      )
    )
  )

  def navbarPara(elemId: String) =
    span(`class` := s"${navbars.Text} footer-text", id := elemId)("")

  def footerIcon(elemId: String, faIcon: String) =
    li(id := elemId, `class` := s"nav-item")(
      a(href := "#", `class` := "nav-link")(iClass(s"fa fa-$faIcon"))
    )
}
