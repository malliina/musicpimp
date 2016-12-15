package com.malliina.musicpimp.tags

import com.malliina.musicpimp.BuildInfo
import com.malliina.musicpimp.models.{Licenses, NewUser}
import com.malliina.musicpimp.tags.Bootstrap._
import com.malliina.musicpimp.tags.PimpTags.callAttr
import com.malliina.musicpimp.tags.Tags._
import com.malliina.play.models.{PasswordChange, Username}
import controllers.{Accounts, routes}
import controllers.routes.Assets.at
import play.api.data.Form
import play.api.mvc.{Call, Flash}

import scalatags.Text.GenericAttr
import scalatags.Text.all._

object PimpTags {
  implicit val callAttr = new GenericAttr[Call]

  def forApp(isProd: Boolean): PimpTags = {
    val scripts = ScalaScripts.forApp(BuildInfo.name, isProd)
    new PimpTags(scripts.optimized, scripts.launcher)
  }

  def withJs(jsFiles: String*): PimpTags =
    new PimpTags(jsFiles.map(file => js(at(file))): _*)
}

class PimpTags(scripts: Modifier*) {

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
          span(`class` := s"uneditable-input $InputMd", id := "user")(username.name)
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
    div(`class` := s"$Lead $alertClass", role := Alert)(message)

  def postableForm(onAction: Call) = form(role := FormRole, action := onAction, method := Post)

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

  def blockSubmitButton = button(`type` := Submit, `class` := s"$BtnPrimary $BtnBlock")

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
    div(`class` := "panel-group", id := "accordion")(
      divClass("panel panel-default")(
        divClass("panel-heading")(
          spanClass("accordion-toggle")(toggleHtml)
        ),
        div(`class` := s"accordion-body $Collapse", id := elemId)(
          divClass("accordion-inner")(
            pre(`class` := "pre-scrollable")(innerContent)
          )
        )
      )
    )

  def panelSummary(prefix: String, elemId: String, linkText: String) =
    aHref(s"#$elemId", dataToggle := Collapse, dataParent := "#accordion")(linkText)

  def indexMain(tabName: String, user: Username)(inner: Modifier*) = {
    def navItem(thisTabName: String, url: Call, glyphiconName: String) = {
      val maybeActive = if (thisTabName.toLowerCase == tabName) Option(`class` := "active") else None
      li(maybeActive)(aHref(url)(glyphIcon(glyphiconName), s" $thisTabName"))
    }

    basePage("MusicPimp")(
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

  def basePage(title: String)(inner: Modifier*) = TagPage(
    html(lang := En)(
      head(
        titleTag(title),
        meta(name := "viewport", content := "width=device-width, initial-scale=1.0"),
        cssLink("//netdna.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"),
        cssLink("//maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css"),
        cssLink("//ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/themes/smoothness/jquery-ui.css"),
        cssLink(at("css/custom.css")),
        cssLink(at("css/footer.css")),
        js("//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"),
        js("//ajax.googleapis.com/ajax/libs/jqueryui/1.10.4/jquery-ui.min.js"),
        js("//netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js")
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
