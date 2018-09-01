package com.malliina.musicpimp.html

import com.malliina.musicpimp.html.PimpHtml.{feedbackDiv, postableForm, stripedHoverTable}
import com.malliina.values.Username
import controllers.musicpimp.{UserFeedback, routes}

import scalatags.Text.all._

object UsersHtml extends PimpBootstrap {

  import tags._

  def usersContent(content: UsersContent) =
    row(
      div6(
        headerDiv(
          h1("Users")
        ),
        stripedHoverTable(Seq("Username", "Actions"))(
          tbody(
            content.us map { u =>
              tr(
                td(u.name),
                td(`class` := "table-button")(postableForm(routes.Accounts.delete(u))(button(`class` := s"${btn.danger} ${btn.sm}")(" Delete")))
              )
            }
          )
        ),
        content.listFeedback.fold(empty)(feedbackDiv)
      ),
      div6(
        headerDiv(
          h1("Add user")
        ),
        addUser(content.addFeedback)
      )
    )

  def accountContent(username: Username, feedback: Option[UserFeedback]) = Seq(
    headerRow("Account", col.md.four),
    rowColumn(col.md.four)(
      changePassword(username, feedback)
    )
  )

  def addUser(addFeedback: Option[UserFeedback]) =
    postableForm(routes.Accounts.formAddUser())(
      inGroup("username", "text", "Username"),
      passwordInputs(),
      blockSubmitButton()("Add User"),
      addFeedback.fold(empty)(feedbackDiv)
    )

  def changePassword(username: Username, feedback: Option[UserFeedback]) =
    postableForm(routes.Accounts.formChangePassword())(
      formGroup(
        labelFor("user")("Username"),
        divClass("controls")(
          spanClass(s"$UneditableInput $InputMd", id := "user")(username.name)
        )
      ),
      passwordGroup("oldPassword", "Old password"),
      passwordInputs("New password", "Repeat new password"),
      blockSubmitButton("Change Password"),
      feedback.fold(empty)(feedbackDiv)
    )

  def passwordInputs(firstLabel: String = "Password", repeatLabel: String = "Repeat password"): Modifier = Seq(
    passwordGroup("newPassword", firstLabel),
    passwordGroup("newPasswordAgain", repeatLabel)
  )

  def passwordGroup(elemId: String, labelText: String) =
    inGroup(elemId, Password, labelText)

  def inGroup(elemId: String, inType: String, labelText: String) =
    formGroup(
      labelFor(elemId)(labelText),
      divClass("controls")(
        namedInput(elemId, `type` := inType, `class` := s"$FormControl $InputMd", required)
      )
    )
}
