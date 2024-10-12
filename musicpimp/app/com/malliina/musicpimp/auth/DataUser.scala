package com.malliina.musicpimp.auth

import com.malliina.values.Username
import io.circe.Codec

case class DataUser(user: Username, passHash: String) derives Codec.AsObject
