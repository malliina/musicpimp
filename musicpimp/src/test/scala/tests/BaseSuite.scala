package tests

import com.malliina.musicpimp.app.LocalConf

trait BaseSuite extends munit.FunSuite:
  val userHome = LocalConf.userHome
