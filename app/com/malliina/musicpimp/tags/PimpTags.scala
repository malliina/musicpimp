package com.malliina.musicpimp.tags

import com.malliina.musicpimp.tags.Bootstrap._
import com.malliina.musicpimp.tags.PimpTags.callAttr
import com.malliina.musicpimp.tags.Tags._
import controllers.routes.Assets.at
import play.api.mvc.Call

import scalatags.Text.GenericAttr
import scalatags.Text.all._

object PimpTags {
  implicit val callAttr = new GenericAttr[Call]

  def forApp(isProd: Boolean): PimpTags = {
    val scripts = ScalaScripts.forApp("musicpimp", isProd)
    new PimpTags(scripts.optimized, scripts.launcher)
  }

  def withJs(jsFiles: String*): PimpTags =
    new PimpTags(jsFiles.map(file => js(at(file))): _*)
}

class PimpTags(scripts: Modifier*) {
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
              spanClass(s"text-muted credit $PullRight")("Developed by ", aHref("https://github.com/malliina", "Michael Skogberg"), ".")
            )
          )
        )
      )
    )
  )
}
