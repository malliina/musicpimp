package com.malliina.musicpimp.tags

import com.malliina.musicpimp.tags.PimpHtml.{feedbackDiv, postableForm, textInputBase}
import com.malliina.play.tags.All._
import controllers.musicpimp.{SettingsController, UserFeedback, routes}

import scalatags.Text.all._

object SettingsHtml {
  def editFolders(folders: Seq[String],
                  folderPlaceholder: String,
                  errorMessage: Option[UserFeedback]) =
    halfRow(
      ulClass(ListUnstyled)(
        folders.map(renderFolder)
      ),
      postableForm(routes.SettingsController.newFolder(), `class` := FormHorizontal, name := "newFolderForm")(
        divClass(InputGroup)(
          spanClass(InputGroupAddon)(glyphIcon("folder-open")),
          textInputBase(Text, SettingsController.Path, Option(folderPlaceholder), `class` := FormControl, required),
          spanClass(InputGroupBtn)(
            submitButton(`class` := BtnPrimary)(glyphIcon("plus"), " Add")
          )
        ),
        errorMessage.fold(empty)(feedbackDiv)
      )
    )

  def renderFolder(folder: String) =
    postableForm(routes.SettingsController.deleteFolder(folder), `class` := FormHorizontal)(
      divClass(InputGroup)(
        spanClass(InputGroupAddon)(glyphIcon("folder-open")),
        spanClass(s"$UneditableInput $FormControl")(folder),
        spanClass(InputGroupBtn)(
          submitButton(`class` := BtnDanger)(glyphIcon("remove"), " Delete")
        )
      )
    )
}
