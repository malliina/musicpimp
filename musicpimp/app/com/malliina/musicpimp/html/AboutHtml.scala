package com.malliina.musicpimp.html

import com.malliina.musicpimp.BuildInfo
import com.malliina.musicpimp.models.Licenses
import com.malliina.play.tags.All._

import scalatags.Text.all._

object AboutHtml {
  def aboutBaseContent = Seq(
    headerRow(ColMd6)("About"),
    rowColumn(ColMd8)(
      leadPara(s"MusicPimp ${BuildInfo.version}"),
      p("Developed by Michael Skogberg."),
      p("Check out ", aHref("https://www.musicpimp.org")("www.musicpimp.org"), " for the latest documentation.")
    ),
    rowColumn(ColMd8)(
      h3("Third Party Software"),
      p("This app uses the following third party software:"),
      divClass("panel-group", id := "accordion")(
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
      prefix, panelSummary(prefix, elemId, linkText)
    )

  def thirdPartyPanel(elemId: String, innerContent: String)(toggleHtml: Modifier*) =
    divClass("panel panel-default")(
      divClass("panel-heading")(
        spanClass("accordion-toggle")(toggleHtml)
      ),
      divClass(s"accordion-body $Collapse", id := elemId)(
        divClass("accordion-inner")(
          pre(`class` := "pre-scrollable")(innerContent)
        )
      )
    )

  def panelSummary(prefix: String, elemId: String, linkText: String) =
    aHref(s"#$elemId", dataToggle := Collapse, dataParent := "#accordion")(linkText)

}
