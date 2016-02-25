package com.malliina.musicpimp.auth

import com.malliina.musicpimp.models.User

case class DataUser(username: User, passwordHash: String)
