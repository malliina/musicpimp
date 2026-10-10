package com.malliina.musicpimp.http4s

import cats.effect.{IO, Resource}
import com.comcast.ip4s.port
import com.malliina.database.Conf
import com.malliina.http.FullUrl
import com.malliina.http.UrlSyntax.url
import com.malliina.http.io.HttpClientIO
import com.malliina.musicpimp.app.{LocalConf, PimpConf}
import com.malliina.values.Password
import munit.AnyFixture
import org.http4s.server.Server

object DatabaseUtils:
  val testConf = LocalConf.local("musicpimp-test.conf")

  private def acquire = IO.delay:
    val either = testConf
      .parse[Password]("musicpimp.db.pass")
      .map: pass =>
        testDatabaseConf(pass)
    either.fold(err => throw err, identity)

  val testDatabase: Resource[IO, Conf] = Resource.make(acquire): conf =>
    IO.unit

  private def testDatabaseConf(password: Password): Conf =
    Conf(
      url"jdbc:mariadb://127.0.0.1:3306/testmusicpimp",
      "testmusicpimp",
      password,
      "org.mariadb.jdbc.Driver",
      2,
      Conf.DefaultMaxLifetime,
      Conf.DefaultKeepaliveTime,
      Conf.DefaultIdleTimeout,
      autoMigrate = true
    )

case class ServerTools(service: Service[IO], server: Server):
  def port = server.address.getPort
  def baseHttpUrl = FullUrl("http", s"localhost:$port", "")
  def baseWsUrl = FullUrl("ws", s"localhost:$port", "")

//trait MUnitDatabaseSuite:
//  self: munit.CatsEffectSuite =>
//
//  val db = ResourceSuiteLocalFixture("database", DatabaseUtils.testDatabase)
//  override def munitFixtures: Seq[AnyFixture[?]] = Seq(db)

trait PimpServerSuite:
  self: munit.CatsEffectSuite =>

  object TestServer extends PimpServerResources
  val http = ResourceFunFixture(HttpClientIO.resource[IO])
  val serverResource =
    for
      testDbConf <- DatabaseUtils.testDatabase
      conf <- Resource.eval(IO.fromEither(PimpConf.parse(_ => testDbConf)))
      app <- TestServer.appResources[IO](conf)
      server <- TestServer.pimpServer[IO](app, port"0")
    yield ServerTools(app, server)
  val server = ResourceSuiteLocalFixture("server", serverResource)
  override def munitFixtures: Seq[AnyFixture[?]] = Seq(server)

abstract class TestServerSuite extends munit.CatsEffectSuite with PimpServerSuite
