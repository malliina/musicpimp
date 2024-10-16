package controllers.pimpcloud

import com.malliina.html.HtmlTags
import com.malliina.musicpimp.audio.{Directory, Folder, Track}
import com.malliina.musicpimp.html.PimpBootstrap
import com.malliina.musicpimp.js.FrontStrings
import com.malliina.musicpimp.js.FrontStrings.{FailStatus, OkStatus}
import com.malliina.musicpimp.models.TrackID
import com.malliina.pimpcloud.CloudStrings
import com.malliina.pimpcloud.tags.ScalaScripts
import com.malliina.play.tags.TagPage
import controllers.Assets.Asset
import controllers.ReverseAssets
import controllers.pimpcloud.CloudTags.versioned
import play.api.mvc.Call
import scalatags.Text.GenericAttr
import scalatags.Text.all.*

object CloudTags:
  val reverseAssets = new ReverseAssets("")

  def at(file: String): Call = reverseAssets.versioned(file)

  implicit val callAttr: GenericAttr[Call] = new GenericAttr[Call]

  /** @param appName
    *   typically the name of the Scala.js module
    * @param isProd
    *   true if the app runs in production, false otherwise
    * @return
    *   HTML templates with either prod or dev javascripts
    */
  def forApp(appName: String, isProd: Boolean): CloudTags =
    val scripts = ScalaScripts.forApp(appName, isProd)
    withLauncher(scripts)

  def withLauncher(scripts: ScalaScripts) = new CloudTags(scripts)

  def versioned(file: Asset) = reverseAssets.versioned(file)

class CloudTags(scripts: ScalaScripts) extends PimpBootstrap with CloudStrings:
  val WideContent = "wide-content"

  import tags.*

  def eject(message: Option[String]) =
    basePage("Goodbye!")(
      divContainer(
        rowColumn(s"${col.md.six} top-padding")(
          message.fold(empty): msg =>
            div(`class` := s"$Lead ${alert.success}", role := alert.Alert)(msg)
        ),
        rowColumn(col.md.six)(
          leadPara("Try to ", a(href := routes.Logs.index)("sign in"), " again.")
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
              action := routes.Web.formAuthenticate,
              method := "POST"
            )(
              h2(`class` := FormSigninHeading)("Please sign in"),
              textInput(Text, FormControl, Web.serverFormKey, "Server", autofocus),
              textInput(Text, FormControl, Web.forms.userFormKey, "Username"),
              textInput(Password, s"$FormControl last-field", Web.forms.passFormKey, "Password"),
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

  def textInput(
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

  def index(dir: Directory, feedback: Option[String]) =
    val feedbackHtml = feedback.fold(empty)(f => fullRow(leadPara(f)))

    def folderHtml(folder: Folder) =
      li(a(href := routes.Phones.folder(folder.id))(folder.title))

    def trackHtml(track: Track) =
      li(
        trackActions(track.id),
        " ",
        a(href := routes.Phones.track(track.id), download)(track.title)
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

  def trackActions(track: TrackID) =
    divClass(btn.group)(
      a(`class` := s"${btn.default} ${btn.sm} $PlayLink", href := "#", id := s"play-$track")(
        iconic("media-play"),
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
            iconic("plus"),
            " Add to playlist"
          )
        ),
        li(
          a(href := routes.Phones.track(track), download)(
            iconic("data-transfer-download"),
            " Download"
          )
        )
      )
    )

  def searchForm(query: Option[String] = None, size: String = InputGroupLg) =
    form(action := routes.Phones.search)(
      divClass(s"$InputGroup $size")(
        input(
          `type` := Text,
          `class` := FormControl,
          placeholder := query.getOrElse("Artist, album or track..."),
          name := "term",
          id := "term"
        ),
        divClass(InputGroupBtn)(
          button(`class` := btn.default, `type` := Submit)(iconic("search"))
        )
      )
    )

  val admin = baseIndex("home")(
    headerRow("Admin"),
    tableContainer(
      "Streams",
      RequestsTableId,
      "Cloud ID",
      "Request ID",
      "Track",
      "Artist",
      "Bytes"
    ),
    tableContainer("Phones", PhonesTableId, "Cloud ID", "Phone Address"),
    tableContainer("Servers", ServersTableId, "Cloud ID", "Server Address")
  )

  def tableContainer(header: String, bodyId: String, headers: String*): Modifier = Seq(
    h2(header),
    fullRow(
      defaultTable(bodyId, headers*)
    )
  )

  def defaultTable(bodyId: String, headers: String*) =
    table(`class` := tables.defaultClass)(
      thead(
        tr(
          headers.map: header =>
            th(header)
        )
      ),
      tbody(id := bodyId)
    )

  def baseIndex(tabName: String, contentClass: String = Container)(inner: Modifier*) =
    def navItem(thisTabName: String, tabId: String, url: Call, iconicName: String) =
      val itemClass = if tabId == tabName then "nav-item active" else "nav-item"
      li(`class` := itemClass)(
        a(href := url, `class` := "nav-link")(iconic(iconicName), s" $thisTabName")
      )

    basePage("pimpcloud")(
      navbar.basic(
        routes.Logs.index,
        "MusicPimp",
        modifier(
          ulClass(s"${navbars.Nav} $MrAuto")(
            navItem("Home", "home", routes.Logs.index, "home"),
            navItem("Logs", "logs", routes.Logs.logs, "list")
          ),
          ulClass(s"${navbars.Nav} ${navbars.Right}")(
            li(`class` := "nav-item")(
              a(href := routes.Logs.logout, `class` := "nav-link")("Logout")
            ),
            div(
              eye(OkStatus, "bolt green"),
              eye(FailStatus, "bolt red")
            )
          )
        )
      ),
      divClass(contentClass)(inner)
    )

  def basePage(title: String)(inner: Modifier*) = TagPage(
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
        cssLink(versioned("styles.css"))
      ),
      body(
        section(
          inner,
          scripts.scripts.map: file =>
            HtmlTags.jsScript(versioned(file))
        )
      )
    )
  )

  def eye(elemId: String, iconicName: String) =
    span(`class` := s"${navbars.Text} ${FrontStrings.HiddenClass}", id := elemId)(
      iconic(iconicName)
    )
