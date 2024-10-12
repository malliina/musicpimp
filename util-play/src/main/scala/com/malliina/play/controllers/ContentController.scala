package com.malliina.play.controllers

import play.api.http.MimeTypes.{HTML, JSON}
import play.api.libs.json.JsValue
import play.api.mvc.Results.{NotAcceptable, Ok}
import play.api.mvc.*
import play.twirl.api.Html

trait ContentController extends Caching:
  def respond(request: RequestHeader)(html: => Result, json: => JsValue): Result =
    respondResult(request)(html, Ok(json))

  def respondResult(request: RequestHeader)(html: => Result, json: => Result): Result =
    val forceJson = request.getQueryString("f").contains("json")
    if forceJson then NoCache(json)
    else respondIgnoreQueryParam(request)(html, NoCache(json))

  /** Browsers may "Accept" anything, so the HTML option is first.
    *
    * Otherwise you might send JSON to a browser that also accepts HTML.
    */
  private def respondIgnoreQueryParam(
    request: RequestHeader
  )(html: => Result, json: => Result): Result =
    if request accepts HTML then html
    else if request accepts JSON then json
    else NotAcceptable

  def response(request: RequestHeader)(html: => Html, json: => JsValue): Result =
    respond(request)(Ok(html), json)
