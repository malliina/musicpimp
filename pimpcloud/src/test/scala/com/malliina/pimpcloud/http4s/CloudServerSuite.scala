package com.malliina.pimpcloud.http4s

import munit.AnyFixture

trait CloudServerSuite extends munit.FunSuite:
  self: munit.CatsEffectSuite =>
  val cloudServer = ResourceSuiteLocalFixture("cloud", CloudServer.server)

  override def munitFixtures: Seq[AnyFixture[?]] = Seq(cloudServer)
