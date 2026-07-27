package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.models.RequestID

case class CloudRequest(message: PimpMessage, request: RequestID)
