package com.malliina.musicpimp.html

import com.malliina.musicpimp.html.PimpHtml.{feedbackDiv, postableForm, textInputBase}
import controllers.musicpimp.{SettingsController, routes}

import scalatags.Text.all._

object SettingsHtml extends PimpBootstrap {

  import tags._

  def editFolders(content: LibraryContent) =
    halfRow(
      ulClass(ListUnstyled)(
        content.folders.map(renderFolder)
      ),
      postableForm(routes.SettingsController.newFolder(), `class` := FormHorizontal, name := "newFolderForm")(
        divClass(InputGroup)(
          spanClass(InputGroupAddon)(iconic("folder")),
          textInputBase(Text, SettingsController.Path, Option(content.folderPlaceholder), `class` := FormControl, required),
          spanClass(InputGroupBtn)(
            submitButton(`class` := {
              btn.primary
            })(iconic("plus"), " Add")
          )
        ),
        content.feedback.fold(empty)(feedbackDiv)
      )
    )

  def renderFolder(folder: String) =
    postableForm(routes.SettingsController.deleteFolder(folder), `class` := FormHorizontal)(
      divClass(InputGroup)(
        spanClass(InputGroupAddon)(iconic("folder")),
        spanClass(s"$UneditableInput $FormControl")(folder),
        spanClass(InputGroupBtn)(
          submitButton(`class` := {
            btn.danger
          })(iconic("delete"), " Delete")
        )
      )
    )
}
