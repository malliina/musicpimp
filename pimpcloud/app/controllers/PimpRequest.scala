package controllers

import com.malliina.musicpimp.json.JsonFormatVersions._
import play.api.http.MimeTypes
import play.api.mvc.RequestHeader

object PimpRequest {
  /**
    * @param request the request
    * @return the desired format of the response to `request`.
    */
  def requestedResponseFormat(request: RequestHeader): Option[String] = {
    //    log.info(s"Headers: ${PlayUtils.headersString(request)}")
    if (request.getQueryString("f").map(_ == "json").isDefined) Some(latest)
    else if (request accepts MimeTypes.HTML) Some(MimeTypes.HTML)
    else if (request accepts anyJson) Some(latest)
    else if (request accepts JSONv17) Some(JSONv17)
    else if ((request accepts JSONv18) || (request accepts MimeTypes.JSON)) Some(JSONv18)
    //    else if ((request accepts JSONv24) || (request accepts MimeTypes.JSON)) Some(JSONv24)
    else None
  }

  /** The desired format for clients compatible with API version 17 is
    * incorrectly determined to be HTML, because those clients do not
    * specify an Accept header in their WebSocket requests thus the server
    * thinks they are browsers by default. However, the WebSocket API does
    * not support HTML, only JSON, so we can safely assume they are JSON
    * clients and since clients newer than version 17 must use the Accept
    * header, we can conclude that they are API version 17 JSON clients.
    *
    * Therefore we can filter out HTML formats as below and default to API
    * version 17 unless the client explicitly requests otherwise.
    *
    * This is a workaround to ensure API compatibility during a transition
    * period from a non-versioned API to a versioned one. Once the transition
    * is complete, we should default to the latest API version.
    */
  def apiVersion(header: RequestHeader) =
    PimpRequest.requestedResponseFormat(header)
      .filter(_ != MimeTypes.HTML)
      .getOrElse(JSONv17)
}
