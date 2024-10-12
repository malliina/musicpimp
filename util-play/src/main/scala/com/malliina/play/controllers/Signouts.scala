package com.malliina.play.controllers

import com.malliina.play.controllers.Signouts.log
import com.malliina.play.json.JsonMessages
import com.malliina.values.Email
import play.api.Logger
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.mvc.*
import com.malliina.http.PlayCirce.jsonWriteable

/** Signout helper */
trait Signouts:
  val messageKey = "message"
  val logoutMessage = "You have successfully signed out."

  def ejectCall: Call

  def onOAuthUnauthorized(email: Email) = ejectWith(unauthorizedMessage(email))

  def eject: Result = ejectWith(logoutMessage)

  def ejectWith(message: String): Result =
    Redirect(ejectCall).flashing(messageKey -> message).withNewSession

  def unauthorizedMessage(email: Email) = s"Hi $email, you're not authorized."

  def noConsentFailure =
    val msg = "The user did not consent to the OAuth request."
    log info msg
    fail(msg)

  def fail(msg: String) = Unauthorized(JsonMessages.failure(msg))

object Signouts:
  private val log = Logger(getClass)
