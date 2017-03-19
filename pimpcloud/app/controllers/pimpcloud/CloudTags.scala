package controllers.pimpcloud

import com.malliina.musicpimp.audio.{Directory, Folder, Track}
import com.malliina.musicpimp.models.TrackID
import com.malliina.pimpcloud.tags.ScalaScripts
import com.malliina.play.tags.All._
import com.malliina.play.tags.TagPage
import controllers.pimpcloud.CloudTags.callAttr
import controllers.routes.Assets.at
import play.api.mvc.Call

import scalatags.Text.GenericAttr
import scalatags.Text.all._

object CloudTags {
  implicit val callAttr = new GenericAttr[Call]

  /**
    * @param appName typically the name of the Scala.js module
    * @param isProd  true if the app runs in production, false otherwise
    * @return HTML templates with either prod or dev javascripts
    */
  def forApp(appName: String, isProd: Boolean): CloudTags = {
    val scripts = ScalaScripts.forApp(appName, isProd)
    withLauncher(scripts.optimized, scripts.launcher)
  }

  def withLauncher(jsFiles: String*) =
    new CloudTags(jsFiles.map(file => jsScript(at(file))): _*)
}

class CloudTags(scripts: Modifier*) {
  val Hidden = "hidden"
  val WideContent = "wide-content"

  def eject(message: Option[String]) =
    basePage("Goodbye!", cssLink(at("css/custom.css")))(
      divContainer(
        rowColumn(s"$ColMd6 top-padding")(
          message.fold(empty) { msg =>
            div(`class` := s"$Lead $AlertSuccess", role := Alert)(msg)
          }
        ),
        rowColumn(ColMd6)(
          leadPara("Try to ", aHref(routes.Logs.index())("sign in"), " again.")
        )
      )
    )

  def login(error: Option[String],
            feedback: Option[String],
            motd: Option[String]) = {
    basePage("Welcome", cssLink(at("css/login.css")))(
      divContainer(
        divClass(s"$ColMd4 wrapper")(
          row(
            feedback.fold(empty)(f => leadPara(f))
          ),
          row(
            form(`class` := FormSignin, name := "loginForm", action := routes.Web.formAuthenticate(), method := "POST")(
              h2(`class` := FormSigninHeading)("Please sign in"),
              textInput(Text, FormControl, Web.serverFormKey, "Server", autofocus),
              textInput(Text, FormControl, Web.forms.userFormKey, "Username"),
              textInput(Password, s"$FormControl last-field", Web.forms.passFormKey, "Password"),
              button(`type` := Submit, id := "loginbutton", `class` := s"$BtnPrimary $BtnLg $BtnBlock")("Sign in")
            )
          ),
          error.fold(empty) { err =>
            row(
              div(`class` := s"$AlertWarning $FormSignin", role := Alert)(err)
            )
          },
          motd.fold(empty) { message =>
            divClass(s"$Row $FormSignin")(
              p(message)
            )
          }
        )
      )
    )
  }

  def textInput(inType: String, clazz: String, idAndName: String, placeHolder: String, more: Modifier*) =
    input(`type` := inType, `class` := clazz, name := idAndName, id := idAndName, placeholder := placeHolder, more)

  val logs = baseIndex("logs", WideContent)(
    headerRow()("Logs"),
    fullRow(
      defaultTable("logTableBody", "Time", "Message", "Logger", "Thread", "Level")
    )
  )

  def index(dir: Directory, feedback: Option[String]) = {
    val feedbackHtml = feedback.fold(empty)(f => fullRow(leadPara(f)))

    def folderHtml(folder: Folder) =
      li(aHref(routes.Phones.folder(folder.id))(folder.title))

    def trackHtml(track: Track) =
      li(trackActions(track.id), " ", a(href := routes.Phones.track(track.id), download)(track.title))

    basePage("Home")(
      divContainer(
        headerRow()("Library"),
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
  }

  def trackActions(track: TrackID) =
    divClass(BtnGroup)(
      a(`class` := s"$BtnDefault $BtnXs play-link", href := "#", id := s"play-$track")(glyphIcon("play"), " Play"),
      a(`class` := s"$BtnDefault $BtnXs $DropdownToggle", dataToggle := Dropdown, href := "#")(spanClass(Caret)),
      ulClass(DropdownMenu)(
        li(a(href := "#", `class` := "playlist-link", id := s"add-$track")(glyphIcon("plus"), " Add to playlist")),
        li(a(href := routes.Phones.track(track), download)(glyphIcon("download"), " Download"))
      )
    )

  def searchForm(query: Option[String] = None, size: String = InputGroupLg) = {
    form(action := routes.Phones.search())(
      divClass(s"$InputGroup $size")(
        input(`type` := Text, `class` := FormControl, placeholder := query.getOrElse("Artist, album or track..."), name := "term", id := "term"),
        divClass(InputGroupBtn)(
          button(`class` := BtnDefault, `type` := Submit)(glyphIcon("search"))
        )
      )
    )
  }

  val admin = baseIndex("home")(
    headerRow()("Admin"),
    tableContainer("Streams", "requestsTable", "Cloud ID", "Request ID", "Track", "Artist", "Bytes"),
    tableContainer("Phones", "phonesTable", "Cloud ID", "Phone Address"),
    tableContainer("Servers", "serversTable", "Cloud ID", "Server Address")
  )

  def tableContainer(header: String, bodyId: String, headers: String*): Modifier = Seq(
    h2(header),
    fullRow(
      defaultTable(bodyId, headers: _*)
    )
  )

  def defaultTable(bodyId: String, headers: String*) =
    table(`class` := TableStripedHover)(
      thead(
        tr(
          headers map { header => th(header) }
        )
      ),
      tbody(id := bodyId)
    )

  def baseIndex(tabName: String, contentClass: String = Container)(inner: Modifier*) = {
    def navItem(thisTabName: String, tabId: String, url: Call, glyphiconName: String) = {
      val maybeActive = if (tabId == tabName) Option(`class` := "active") else None
      li(maybeActive)(a(href := url)(glyphIcon(glyphiconName), s" $thisTabName"))
    }

    basePage("pimpcloud")(
      divClass(s"$Navbar $NavbarDefault")(
        divContainer(
          divClass(NavbarHeader)(
            hamburgerButton,
            a(`class` := NavbarBrand, href := routes.Logs.index())("MusicPimp")
          ),
          divClass(s"$NavbarCollapse $Collapse")(
            ulClass(s"$Nav $NavbarNav")(
              navItem("Home", "home", routes.Logs.index(), "home"),
              navItem("Logs", "logs", routes.Logs.logs(), "list")
            ),
            ulClass(s"$Nav $NavbarNav $NavbarRight")(
              li(aHref(routes.AdminAuth.logout())("Logout"))
            ),
            divClass(s"$ColMd2 $PullRight")(
              eye("okstatus", "eye-open green"),
              eye("failstatus", "eye-close red")
            )
          )
        )
      ),
      divClass(contentClass)(inner)
    )
  }

  def basePage(title: String, extraHeader: Modifier*)(inner: Modifier*) = TagPage(
    html(lang := En)(
      head(
        titleTag(title),
        meta(name := "viewport", content := "width=device-width, initial-scale=1.0"),
        cssLink("//netdna.bootstrapcdn.com/bootstrap/3.3.4/css/bootstrap.min.css"),
        cssLink("//netdna.bootstrapcdn.com/font-awesome/3.2.1/css/font-awesome.css"),
        cssLink("//ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/themes/smoothness/jquery-ui.css"),
        cssLink(at("css/custom.css")),
        extraHeader,
        jsScript("//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"),
        jsScript("//ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/jquery-ui.min.js"),
        jsScript("//netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js")
      ),
      body(
        section(
          inner,
          scripts
        )
      )
    )
  )

  def eye(elemId: String, glyphSuffix: String) =
    pClass(s"$NavbarText $PullRight $HiddenXs $Hidden", id := elemId)(glyphIcon(glyphSuffix))
}
