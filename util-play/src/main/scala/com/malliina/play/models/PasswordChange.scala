package com.malliina.play.models

import com.malliina.values.Password

case class PasswordChange(oldPass: Password, newPass: Password, newPassAgain: Password)
