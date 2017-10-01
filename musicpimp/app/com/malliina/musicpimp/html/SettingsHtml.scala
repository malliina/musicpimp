package com.malliina.musicpimp.html

import com.malliina.musicpimp.html.PimpHtml.{feedbackDiv, postableForm, textInputBase}
import com.malliina.play.tags.All._
import controllers.musicpimp.{SettingsController, routes}

import scalatags.Text.all._

object SettingsHtml {
  def editFolders(content: LibraryContent) =
    halfRow(
      ulClass(ListUnstyled)(
        content.folders.map(renderFolder)
      ),
      postableForm(routes.SettingsController.newFolder(), `class` := FormHorizontal, name := "newFolderForm")(
        divClass(InputGroup)(
          spanClass(InputGroupAddon)(glyphIcon("folder-open")),
          textInputBase(Text, SettingsController.Path, Option(content.folderPlaceholder), `class` := FormControl, required),
          spanClass(InputGroupBtn)(
            submitButton(`class` := BtnPrimary)(glyphIcon("plus"), " Add")
          )
        ),
        content.feedback.fold(empty)(feedbackDiv)
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
