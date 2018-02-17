package com.malliina.musicpimp.html

import com.malliina.musicpimp.BuildInfo
import com.malliina.musicpimp.models.Licenses

import scalatags.Text.all._

object AboutHtml extends PimpBootstrap {

  import tags.{False, divClass}

  val accordionId = "accordion"

  def aboutBaseContent = Seq(
    headerRow("About", col.md.six),
    rowColumn(col.md.eight)(
      leadPara(s"MusicPimp ${BuildInfo.version}"),
      p("Developed by Michael Skogberg."),
      p("Check out ", a(href := "https://www.musicpimp.org")("www.musicpimp.org"), " for the latest documentation.")
    ),
    rowColumn(col.md.eight)(
      h3("Third Party Software"),
      p("This app uses the following third party software:"),
      divClass("panel-group", id := accordionId)(
        licensePanel("one", Licenses.SCALA, "Scala, licensed under the ", "Scala License"),
        licensePanel("two", Licenses.MIT, "software licensed under the ", "MIT License"),
        licensePanel("three", Licenses.APACHE, "software licensed under ", "Apache License 2.0"),
        licensePanel("four", Licenses.LGPL, "Tritonus plugins, licensed under the ", "GNU Lesser General Public License (LGPL)")
      ),
      p("... and icons by ", a(href := "https://useiconic.com/open")("Iconic"), ".")
    )
  )

  def licensePanel(elemId: String, licenseText: String, prefix: String, linkText: String) =
    thirdPartyPanel(elemId, licenseText)(
      prefix, panelSummary(prefix, elemId, linkText)
    )

  def thirdPartyPanel(elemId: String, innerContent: String)(toggleHtml: Modifier*) = {
    val headingId = s"heading-$elemId"
    divClass("card")(
      divClass("card-header", id := headingId)(
        h5(`class` := "mb-0")(
          toggleHtml
        )
      ),
      divClass(s"accordion-body $Collapse", id := s"collapse-$elemId", aria.labelledby := headingId)(
        divClass("accordion-inner")(
          pre(`class` := "pre-scrollable card-body")(innerContent)
        )
      )
    )
  }


  def panelSummary(prefix: String, elemId: String, linkText: String) =
    button(`class` := "btn btn-link collapsed", dataTarget := s"#collapse-$elemId", dataToggle := Collapse, dataParent := s"#$accordionId", aria.expanded := False)(linkText)
}
