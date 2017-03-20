package com.malliina.musicpimp.tags

import com.malliina.musicpimp.js.CloudStrings
import com.malliina.musicpimp.models.CloudID
import com.malliina.musicpimp.tags.PimpHtml.postableForm
import com.malliina.play.tags.All._
import controllers.musicpimp.{Cloud, routes}

import scalatags.Text.all._

object CloudHtml extends CloudStrings {
  def cloudContent = Seq(
    headerRow()("Cloud"),
    div(id := CloudForm)(
      leadPara("Connecting...")
    ),
    halfRow(
      p("How does this work?"),
      p("This server will open a connection to a machine on the internet. Your mobile device connects to the " +
        "same machine on the internet and communicates with this server through the machine both parties have " +
        "connected to. All traffic is encrypted. All music is streamed.")
    )
  )

  def cloudForm(cloudId: Option[CloudID]) = {
    val title = cloudId.fold("Connect")(_ => "Disconnect")
    postableForm(routes.Cloud.toggle(), name := "toggleForm")(
      if (cloudId.isEmpty) {
        formGroup(
          labelFor(Cloud.idFormKey)("Desired cloud ID (optional)"),
          textInput(Text, FormControl, Cloud.idFormKey, Option("Your desired ID or leave empty"))
        )
      } else {
        empty
      },
      blockSubmitButton(id := ToggleButton)(title)
    )
  }

  def textInput(inType: String,
                clazz: String,
                idAndName: String,
                placeHolder: Option[String],
                more: Modifier*) =
    PimpHtml.textInputBase(inType, idAndName, placeHolder, `class` := clazz, more)
}
