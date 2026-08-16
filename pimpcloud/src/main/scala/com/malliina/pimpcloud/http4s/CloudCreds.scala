package com.malliina.pimpcloud.http4s

import com.malliina.musicpimp.models.CloudID
import com.malliina.values.{Password, Username}

case class CloudCreds(cloudID: CloudID, username: Username, pass: Password)
