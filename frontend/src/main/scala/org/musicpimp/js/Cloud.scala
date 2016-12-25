package org.musicpimp.js

import com.malliina.tags.Bootstrap._
import com.malliina.tags.Tags._
import org.scalajs.dom.raw.Event
import upickle.Js.Value

import scalatags.Text.all._
import scalatags.Text.{Frag, TypedTag}

case class UserFeedback(message: String, isError: Boolean)

case class SocketEvent(event: String, id: Option[String]) {
  def isConnected = id.isDefined
}

object Cloud {
  val inputId = "id"

  def connectedContent(id: String): Frag = {
    val msg = s"Connected. You can now access this server using your credentials and this cloud ID: $id"
    SeqFrag(Seq(
      halfRow(connectedForm()),
      halfRow(alertSuccess(msg))
    ))
  }

  def disconnectedContent: Frag = {
    SeqFrag(Seq(
      halfRow(disconnectedForm()),
      halfRow(alertDanger("Not connected."))
    ))
  }

  private def disconnectedForm() = cloudForm(
    formGroup(
      labelFor(inputId)("Desired cloud ID (optional)"),
      input(`type` := Text,
        id := inputId,
        name := inputId,
        placeholder := "Your desired ID or leave empty",
        `class` := FormControl)
    ),
    cloudButton("Connect")
  )

  private def connectedForm() = cloudForm(
    cloudButton("Disconnect")
  )

  private def cloudButton(title: String) =
    blockSubmitButton(id := "toggleButton")(title)

  private def cloudForm = postableForm("/cloud", name := "toggleForm")

  private def postableForm(onAction: String, more: Modifier*) =
    form(role := FormRole, action := onAction, method := Post, SeqNode(more))

  private def feedbackDiv(feedback: UserFeedback): TypedTag[String] = {
    val message = feedback.message
    if (feedback.isError) alertDanger(message)
    else alertSuccess(message)
  }
}

class Cloud extends SocketJS("/ws/cloud?f=json") {
  val formDiv = elem("cloud-form")

  override def onConnected(e: Event) = {
    send(Command.subscribe)
    super.onConnected(e)
  }

  override def handlePayload(payload: Value) = {
    validate[SocketEvent](payload).fold[Any](onInvalidData, onSocketEvent)
  }

  def onSocketEvent(event: SocketEvent) = {
    event.event match {
      case "cloud" =>
        val content = event.id
          .map(Cloud.connectedContent)
          .getOrElse(Cloud.disconnectedContent)
        formDiv.html(content.render)
      case other =>
        println(s"Unknown event: '$other'")
    }
  }
}
