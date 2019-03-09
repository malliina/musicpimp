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
      postableForm(routes.SettingsController.newFolder(),
                   `class` := FormHorizontal,
                   name := "newFolderForm")(
        divClass(InputGroup)(
          divClass("input-group-prepend")(
            spanClass("input-group-text")(iconic("folder")),
          ),
          textInputBase(Text,
                        SettingsController.Path,
                        Option(content.folderPlaceholder),
                        `class` := FormControl,
                        required),
          divClass("input-group-append")(
            submitButton(`class` := btn.primary)(iconic("plus"), " Add")
          )
        ),
        content.feedback.fold(empty)(feedbackDiv)
      )
    )

  def renderFolder(folder: String) =
    postableForm(routes.SettingsController.deleteFolder(folder), `class` := FormHorizontal)(
      divClass(InputGroup)(
        divClass("input-group-prepend")(
          spanClass("input-group-text")(iconic("folder")),
        ),
        spanClass(s"$FormControl", readonly)(folder),
        divClass("input-group-append")(
          submitButton(`class` := btn.danger)(iconic("delete"), " Delete")
        )
      )
    )
}
