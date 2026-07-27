package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.models.CloudID
import io.circe.Codec

case class RegistrationEvent(event: String, id: CloudID) extends PimpMessage derives Codec.AsObject
