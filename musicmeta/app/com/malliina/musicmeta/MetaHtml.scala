package com.malliina.musicmeta

import com.malliina.html.{Bootstrap, Tags}
import com.malliina.play.tags.TagPage
import controllers.routes
import controllers.routes.MetaAssets.versioned
import play.api.Mode
import play.api.Mode.Prod
import play.api.mvc.Call
import scalatags.Text.GenericAttr
import scalatags.Text.all._

object MetaHtml {
  def apply(appName: String, mode: Mode): MetaHtml = {
    val suffix = if (mode == Prod) "opt" else "fastopt"
    new MetaHtml(s"$appName-$suffix.js")
  }
}

class MetaHtml(jsName: String) extends Bootstrap(Tags) {

  import tags._

  implicit val callAttr: GenericAttr[Call] = new GenericAttr[Call]
  val empty: Modifier = ()

  def logs(feedback: Option[UserFeedback]) = baseIndex("logs", wide = true)(
    headerRow("Logs"),
    fullRow(
      feedback.fold(empty)(feedbackDiv),
      span(id := "status", `class` := Lead)("Initializing..."),
      divClass(s"${btn.group} btn-group-toggle compact-group float-right", role := "group", data("toggle") := "buttons")(
        label(`class` := s"${btn.info} ${btn.sm}", id := "label-verbose")(
          input(`type` := "radio", name := "options", id := "option-verbose", autocomplete := "off")(" Verbose"),
        ),
        label(`class` := s"${btn.info} ${btn.sm} active", id := "label-compact")(
          input(`type` := "radio", name := "options", id := "option-compact", autocomplete := "off")(" Compact"),
        ),
      ),
    ),
    fullRow(
      table(`class` := tables.defaultClass)(
        thead(tr(th("Time"), th("Message"), th(`class` := "verbose off")("Logger"), th("Level"))),
        tbody(id := "log-table-body")
      )
    ),
    jsScript(versioned(jsName))
  )

  def eject(feedback: Option[UserFeedback]) =
    basePage("Goodbye!")(
      divContainer(
        halfRow(
          feedback.fold(empty)(feedbackDiv),
          p("Try to ", a(href := routes.MetaOAuth.logs())("sign in"), " again.")
        )
      )
    )

  def baseIndex(tabName: String, wide: Boolean)(content: Modifier*) = {
    def navItem(thisTabName: String, tabId: String, url: Call, iconicName: String) = {
      val itemClass = if (tabId == tabName) "nav-item active" else "nav-item"
      li(`class` := itemClass)(a(href := url, `class` := "nav-link")(iconic(iconicName), s" $thisTabName"))
    }

    basePage("MusicPimp")(
      navbar.basic(
        routes.MetaOAuth.index(),
        "musicmeta",
        modifier(
          ulClass(s"${navbar.Nav} $MrAuto")(
            navItem("Logs", "logs", routes.MetaOAuth.logs(), "list")
          ),
          ulClass(s"${navbar.Nav} ${navbar.Right}")(
            li(`class` := "nav-item")(a(href := routes.MetaOAuth.logout(), `class` := "nav-link")("Logout"))
          )
        )
      ),
      (if (wide) divClass("wide-content") else divContainer) (content)
    )
  }

  def basePage(title: String)(content: Modifier*) = TagPage(
    html(lang := En)(
      head(
        titleTag(title),
        deviceWidthViewport,
        cssLinkHashed("https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css", "sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm"),
        cssLink("https://use.fontawesome.com/releases/v5.0.6/css/all.css"),
        cssLink(versioned("css/main.css")),
        jsHashed("https://code.jquery.com/jquery-3.2.1.slim.min.js", "sha384-KJ3o2DKtIkvYIK3UENzmM7KCkRr/rE9/Qpg6aAZGJwFDMVNA/GpGFF93hXpG5KkN"),
        jsHashed("https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.12.9/umd/popper.min.js", "sha384-ApNbgh9B+Y1QKtv3Rn7W3mgPxhU9K/ScQsAP7hUibX39j7fakFPskvXusvfa0b4Q"),
        jsHashed("https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/js/bootstrap.min.js", "sha384-JZR6Spejh4U02d8jOt6vLEHfe/JQGiRRSQQxSfFWpi1MquVdAyjUar5+76PVCmYl")
      ),
      body(
        content,
        footer(`class` := "footer")(
          divClass(Container)(
            spanClass(s"${text.muted} float-right")("Developed by ", a(href := "https://github.com/malliina")("Michael Skogberg"), ".")
          )
        )
      )
    )
  )

  def feedbackDiv(feedback: UserFeedback) = {
    val message = feedback.message
    if (feedback.isError) alertDanger(message)
    else alertSuccess(message)
  }
}
