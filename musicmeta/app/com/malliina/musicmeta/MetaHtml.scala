package com.malliina.musicmeta

import com.malliina.html.{Bootstrap, HtmlTags}
import com.malliina.play.tags.TagPage
import controllers.routes
import controllers.routes.MetaAssets.versioned
import play.api.Mode
import play.api.mvc.Call
import scalatags.Text.GenericAttr
import scalatags.Text.all._

case class ScalaScripts(jsFiles: Seq[String])

object ScalaScripts {

  /**
    * @param appName typically the name of the Scala.js module
    * @param isProd  true if the app runs in production, false otherwise
    */
  def forApp(appName: String, isProd: Boolean): ScalaScripts = {
    val name = appName.toLowerCase
    val opt = if (isProd) "opt" else "fastopt"
    ScalaScripts(Seq(s"$name-$opt-library.js", s"$name-$opt-loader.js", s"$name-$opt.js"))
  }
}


object MetaHtml {
  def apply(appName: String, mode: Mode): MetaHtml =
    new MetaHtml(ScalaScripts.forApp(appName, mode == Mode.Prod))
}

class MetaHtml(scripts: ScalaScripts) extends Bootstrap(HtmlTags) {

  import tags._

  implicit val callAttr: GenericAttr[Call] = new GenericAttr[Call]
  val empty: Modifier = ()

  def logs(feedback: Option[UserFeedback]) = baseIndex("logs", wide = true)(
    headerRow("Logs"),
    fullRow(
      feedback.fold(empty)(feedbackDiv),
      span(id := "status", `class` := Lead)("Initializing..."),
      divClass(s"${btn.group} btn-group-toggle compact-group float-right",
               role := "group",
               data("toggle") := "buttons")(
        label(`class` := s"${btn.info} ${btn.sm}", id := "label-verbose")(
          input(`type` := "radio",
                name := "options",
                id := "option-verbose",
                autocomplete := "off")(" Verbose"),
        ),
        label(`class` := s"${btn.info} ${btn.sm} active", id := "label-compact")(
          input(`type` := "radio",
                name := "options",
                id := "option-compact",
                autocomplete := "off")(" Compact"),
        ),
      ),
    ),
    fullRow(
      table(`class` := tables.defaultClass)(
        thead(tr(th("Time"), th("Message"), th(`class` := "verbose off")("Logger"), th("Level"))),
        tbody(id := "log-table-body")
      )
    ),
    scripts.jsFiles.map { file => jsScript(versioned(file)) }
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
      li(`class` := itemClass)(
        a(href := url, `class` := "nav-link")(iconic(iconicName), s" $thisTabName"))
    }

    basePage("MusicPimp")(
      navbar.basic(
        routes.MetaOAuth.index(),
        "musicmeta",
        modifier(
          ulClass(s"${navbars.Nav} $MrAuto")(
            navItem("Logs", "logs", routes.MetaOAuth.logs(), "list")
          ),
          ulClass(s"${navbars.Nav} ${navbars.Right}")(
            li(`class` := "nav-item")(
              a(href := routes.MetaOAuth.logout(), `class` := "nav-link")("Logout"))
          )
        )
      ),
      (if (wide) divClass("wide-content") else divContainer)(content)
    )
  }

  def basePage(title: String)(content: Modifier*) = TagPage(
    html(lang := En)(
      head(
        titleTag(title),
        deviceWidthViewport,
        cssLinkHashed("https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0/css/bootstrap.min.css",
                      "sha384-Gn5384xqQ1aoWXA+058RXPxPg6fy4IWvTNh0E263XmFcJlSAwiGgFAW/dAiS6JXm"),
        cssLink("https://use.fontawesome.com/releases/v5.0.6/css/all.css"),
        cssLink(versioned("styles.css")),
      ),
      body(
        content,
        footer(`class` := "footer")(
          divClass(Container)(
            spanClass(s"${text.muted} float-right")(
              "Developed by ",
              a(href := "https://github.com/malliina")("Michael Skogberg"),
              ".")
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

  def iconic(iconicName: String) = spanClass(s"oi oi-$iconicName", title := iconicName, aria.hidden := True)
}
