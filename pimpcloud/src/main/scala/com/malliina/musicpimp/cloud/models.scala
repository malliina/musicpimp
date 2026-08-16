package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.models.RequestID
import io.circe.{Codec, Json}

case class BodyAndId(body: Json, request: RequestID) derives Codec.AsObject

class RequestFailure(val response: Json) extends Exception
