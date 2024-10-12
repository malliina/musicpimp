package tests

import munit.{FunSuite, Suite}
import play.api.ApplicationLoader.Context
import play.api.test.{DefaultTestServerFactory, RunningServer}
import play.api.{BuiltInComponents, Play}

abstract class AppSuite[T <: BuiltInComponents](build: Context => T)
  extends FunSuite
  with AppPerSuite[T] {
  override def createComponents(context: Context): T = build(context)
}

abstract class ServerSuite[T <: BuiltInComponents](build: Context => T)
  extends FunSuite
  with ServerPerSuite[T] {
  override def createComponents(context: Context): T = build(context)
}

trait PlayAppFixture[T <: BuiltInComponents] { self: FunSuite =>
  def components(context: Context): T
  lazy val components: T = components(TestAppLoader.createTestAppContext)
  val app = FunFixture[T](
    opts => {
      Play.start(components.application)
      components
    },
    comps => {
      Play.stop(components.application)
    }
  )
}

trait PlayServerFixture[T <: BuiltInComponents] { self: FunSuite =>
  def components(context: Context): T
  lazy val components: T = components(TestAppLoader.createTestAppContext)
  val server = FunFixture[RunningServer](
    opts => {
      DefaultTestServerFactory.start(components.application)
    },
    running => {
      running.stopServer.close()
    }
  )
}

trait AppPerSuite[T <: BuiltInComponents] { self: Suite =>
  def createComponents(context: Context): T
  lazy val components = createComponents(TestAppLoader.createTestAppContext)
  val testApp: Fixture[T] = new Fixture[T]("test-app") {
    private var comps: Option[T] = None
    def apply() = comps.get
    override def beforeAll(): Unit = {
      comps = Option(components)
      Play.start(components.application)
    }
    override def afterAll(): Unit = {
      comps.foreach(c => Play.stop(c.application))
    }
  }
  lazy val app = testApp().application

  override def munitFixtures = Seq(testApp)
}

trait ServerPerSuite[T <: BuiltInComponents] { self: Suite =>
  def createComponents(context: Context): T
  lazy val components = createComponents(TestAppLoader.createTestAppContext)
  val testServer: Fixture[RunningServer] = new Fixture[RunningServer]("test-server") {
    private var runningServer: RunningServer = null
    def apply() = runningServer
    override def beforeAll(): Unit = {
      runningServer = DefaultTestServerFactory.start(components.application)
    }
    override def afterAll(): Unit = {
      runningServer.stopServer.close()
    }
  }
  def port = testServer().endpoints.httpEndpoint.map(_.port).get

  override def munitFixtures = Seq(testServer)
}
