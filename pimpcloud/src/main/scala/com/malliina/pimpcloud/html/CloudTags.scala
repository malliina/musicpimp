package com.malliina.pimpcloud.html

import com.malliina.html.HtmlTags
import com.malliina.musicpimp.audio.{Directory, Folder, Track}
import com.malliina.musicpimp.html.PimpBootstrap
import com.malliina.musicpimp.js.FrontStrings
import com.malliina.musicpimp.js.FrontStrings.{FailStatus, OkStatus}
import com.malliina.musicpimp.models.TrackID
import com.malliina.pimpcloud.{BuildInfo, CloudStrings}
import com.malliina.pimpcloud.html.CloudTags.{at, given}
import com.malliina.pimpcloud.http4s.{AccountKeys, Reverse, Web}
import com.malliina.pimpcloud.tags.ScalaScripts
import com.malliina.musicpimp.html.TagPage
import org.http4s.Uri
import org.http4s.implicits.uri
import scalatags.Text.all.*
import scalatags.text.Builder

object CloudTags:
  given AttrValue[Uri] = attrValue(_.renderString)

  private def attrValue[T](f: T => String): AttrValue[T] =
    (t: Builder, a: Attr, v: T) => t.setAttr(a.name, Builder.GenericAttrValueSource(f(v)))

  def at(file: String): Uri = uri"/assets".addPath(file)

  def default = forApp(BuildInfo.frontName, BuildInfo.isProd)

  /** @param appName
    *   typically the name of the Scala.js module
    * @param isProd
    *   true if the app runs in production, false otherwise
    * @return
    *   HTML templates with either prod or dev javascripts
    */
  private def forApp(appName: String, isProd: Boolean): CloudTags =
    val scripts = ScalaScripts.forApp(appName, isProd)
    withLauncher(scripts)

  private def withLauncher(scripts: ScalaScripts) = new CloudTags(scripts)

class CloudTags(scripts: ScalaScripts) extends PimpBootstrap with CloudStrings:
  val reverse = Reverse
  private val WideContent = "wide-content"

  import tags.*

  def eject(message: Option[String]) =
    basePage("Goodbye!")(
      divContainer(
        rowColumn(s"${col.md.six} top-padding")(
          message.fold(empty): msg =>
            div(`class` := s"$Lead ${alert.success}", role := alert.Alert)(msg)
        ),
        rowColumn(col.md.six)(
          leadPara("Try to ", a(href := reverse.admin.base)("sign in"), " again.")
        )
      )
    )

  def login(error: Option[String], feedback: Option[String], motd: Option[String]) =
    val formWidth = s"${col.md.eight} ${col.lg.six}"
    basePage("Welcome")(
      divContainer(
        divClass("wrapper login-container")(
          row(
            feedback.fold(empty)(f => p(`class` := s"$formWidth $Lead")(f))
          ),
          row(
            form(
              `class` := s"$FormSignin $formWidth",
              name := "loginForm",
              action := reverse.authenticate,
              method := "POST"
            )(
              h2(`class` := FormSigninHeading)("Please sign in"),
              textInput(Text, FormControl, Web.serverFormKey, "Server", autofocus),
              textInput(Text, FormControl, AccountKeys.userFormKey, "Username"),
              textInput(Password, s"$FormControl last-field", AccountKeys.passFormKey, "Password"),
              button(
                `type` := Submit,
                id := "loginbutton",
                `class` := s"${btn.primary} ${btn.lg} ${btn.block}"
              )("Sign in")
            )
          ),
          error.fold(empty): err =>
            row(
              divClass(s"$FormSignin $formWidth")(
                div(`class` := alert.warning, role := alert.Alert)(err)
              )
            ),
          motd.fold(empty): message =>
            divClass(s"$Row $FormSignin")(
              p(`class` := col.lg.six, message)
            )
        )
      )
    )

  private def textInput(
    inType: String,
    clazz: String,
    idAndName: String,
    placeHolder: String,
    more: Modifier*
  ) =
    input(
      `type` := inType,
      `class` := clazz,
      name := idAndName,
      id := idAndName,
      placeholder := placeHolder,
      more
    )

  val logs = baseIndex("logs", WideContent)(
    headerRow("Logs"),
    fullRow(
      defaultTable("logTableBody", "Time", "Message", "Logger", "Thread", "Level")
    )
  )

  def index(dir: Directory, feedback: Option[String]): TagPage =
    val feedbackHtml = feedback.fold(empty)(f => fullRow(leadPara(f)))

    def folderHtml(folder: Folder) =
      li(a(href := reverse.folders.folder(folder.id))(folder.title))

    def trackHtml(track: Track) =
      li(
        trackActions(track.id),
        " ",
        a(href := reverse.downloads.download(track.id), download)(track.title)
      )

    basePage("Home")(
      divContainer(
        headerRow("Library"),
        fullRow(
          searchForm()
        ),
        fullRow(
          p(id := "status")
        ),
        feedbackHtml,
        fullRow(
          ulClass(ListUnstyled)(
            dir.folders map folderHtml,
            dir.tracks map trackHtml
          )
        )
      )
    )

  private def trackActions(track: TrackID) =
    divClass(btn.group)(
      a(`class` := s"${btn.default} ${btn.sm} $PlayLink", href := "#", id := s"play-$track")(
        faIcon("play"),
        " Play"
      ),
      a(
        `class` := s"${btn.default} ${btn.sm} $DropdownToggle",
        dataToggle := Dropdown,
        href := "#"
      )(spanClass(Caret)),
      ulClass(DropdownMenu)(
        li(
          a(href := "#", `class` := PlaylistLink, id := s"add-$track")(
            faIcon("plus"),
            " Add to playlist"
          )
        ),
        li(
          a(href := reverse.downloads.download(track), download)(
            faIcon("download"),
            " Download"
          )
        )
      )
    )

  private def searchForm(query: Option[String] = None, size: String = InputGroupLg) =
    form(action := reverse.search)(
      divClass(s"$InputGroup $size")(
        input(
          `type` := Text,
          `class` := FormControl,
          placeholder := query.getOrElse("Artist, album or track..."),
          name := "term",
          id := "term"
        ),
        button(`class` := btn.default, `type` := Submit)(faIcon("search"))
      )
    )

  val admin = baseIndex("home")(
    headerRow("Admin"),
    tableContainer(
      "Streams",
      RequestsTableElemId,
      RequestsTableId,
      "Cloud ID",
      "Request ID",
      "Track",
      "Artist",
      "Bytes"
    ),
    tableContainer("Phones", PhonesTableElemId, PhonesTableId, "Cloud ID", "Phone Address"),
    tableContainer("Servers", ServersTableElemId, ServersTableId, "Cloud ID", "Server Address")
  )

  private def tableContainer(
    header: String,
    tableId: String,
    bodyId: String,
    headers: String*
  ): Modifier =
    Seq(
      h2(header),
      fullRow(
        defaultTable(tableId, bodyId, headers*)
      )
    )

  private def defaultTable(tableId: String, bodyId: String, headers: String*) =
    table(`class` := tables.defaultClass, id := tableId)(
      thead(
        tr(
          headers.map: header =>
            th(header)
        )
      ),
      tbody(id := bodyId)
    )

  private def baseIndex(tabName: String, contentClass: String = Container)(inner: Modifier*) =
    def navItem(thisTabName: String, tabId: String, url: Uri, faName: String) =
      val itemClass = if tabId == tabName then "nav-item active" else "nav-item"
      li(`class` := itemClass)(
        a(href := url, `class` := "nav-link")(faIcon(faName), s" $thisTabName")
      )

    basePage("pimpcloud")(
      navbar.basic(
        reverse.admin.base,
        "MusicPimp",
        modifier(
          ulClass(s"${navbars.Nav} $MrAuto")(
            navItem("Home", "home", reverse.admin.base, "home"),
            navItem("Logs", "logs", reverse.admin.logs, "list")
          ),
          ulClass(s"${navbars.Nav} ${navbars.Right}")(
            li(`class` := "nav-item")(
              a(href := reverse.admin.logout, `class` := "nav-link")("Logout")
            ),
            div(
              eye(OkStatus, "eye green"),
              eye(FailStatus, "eye red")
            )
          )
        )
      ),
      divClass(contentClass)(inner)
    )

  private def basePage(title: String)(inner: Modifier*) = TagPage(
    html(lang := En)(
      head(
        titleTag(title),
        deviceWidthViewport,
        cssLinkHashed(
          "https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css",
          "sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm"
        ),
        cssLink("https://use.fontawesome.com/releases/v5.0.6/css/all.css"),
        cssLink("//maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css"),
        cssLink("https://code.jquery.com/ui/1.12.1/themes/base/jquery-ui.css"),
        cssLink(at("styles.css"))
      ),
      body(
        section(
          inner,
          scripts.scripts.map: file =>
            HtmlTags.jsScript(at(file), defer)
        )
      )
    )
  )

  private def eye(elemId: String, faName: String) =
    span(`class` := s"${navbars.Text} ${FrontStrings.HiddenClass}", id := elemId)(
      faIcon(faName)
    )
