package com.malliina.play

import java.nio.file.{Files, Path, Paths}
import java.rmi.ConnectException

import com.malliina.concurrent.ExecutionContexts
import com.malliina.file.FileUtilities
import com.malliina.play.PlayLifeCycle.log
import com.malliina.rmi.{RmiClient, RmiServer, RmiUtil}
import com.malliina.security.KeyStores.{keyStoreKey, validateKeyStoreIfSpecified}
import com.malliina.util.Util
import play.api.Logger
import play.core.server.{ProdServerStart, RealServerProcess, ReloadableServer}

import scala.concurrent.Future

/** Starts Play Framework 2, does not create a RUNNING_PID file.
  *
  * An alternative to the official ways to start Play, this integrates better with my init scripts.
  */
abstract class PlayLifeCycle(appName: String, registryPort: Int) {
  protected val (httpPortKey, httpsPortKey, httpAddressKey) =
    ("http.port", "https.port", "http.address")
  protected val defaultHttpPort = 9000
  protected val defaultHttpAddress = "0.0.0.0"

  var server: Option[ReloadableServer] = None
  var rmiServer: Option[RmiServer] = None

  def main(args: Array[String]) {
    args.headOption match {
      case Some("stop") =>
        try {
          RmiClient.launchClient()
          // this hack allows the System.exit() call in the stop method eventually to run before we exit
          // obviously we can't await it from another vm
          Thread sleep 2000
        } catch {
          case _: ConnectException =>
            log warn "Unable to stop; perhaps MusicPimp is already stopped?"
        }
      case anythingElse => start()
    }
  }

  def start(): Unit = {
    // Init conf
    sys.props += ("pidfile.path" -> "/dev/null")
    FileUtilities.basePath = Paths get sys.props.get(s"$appName.home").getOrElse(sys.props("user.dir"))
    log info s"Starting $appName... app home: ${FileUtilities.basePath}"
    sys.props ++= conformize(readConfFile(appName))
    validateKeyStoreIfSpecified()
    // Start server
    val process = new RealServerProcess(Nil)
    val s = ProdServerStart.start(process)
    server = Some(s)
    // Init RMI
    RmiUtil.initSecurityPolicy()
    rmiServer = Some(new RmiServer(registryPort) {
      override def onClosed(): Unit = stop()
    })
  }

  def stop(): Unit = {
    log info s"Stopping $appName..."
    try {
      server.foreach(_.stop())
    } finally {
      /** Well the following is lame, but some threads are left hanging
        * and I don't know how to stop them gracefully.
        */
      Future(System.exit(0))(ExecutionContexts.cached)
    }
  }

  /** Reads a file named `confNameWithoutExtension`.conf if it exists.
    *
    * @param confNameWithoutExtension name of conf, typically the name of the app
    * @return the key-value pairs from the conf file; an empty map if the file doesn't exist
    */
  def readConfFile(confNameWithoutExtension: String): Map[String, String] = {
    // adds settings in app conf to system properties
    val confFile = FileUtilities.pathTo(s"$confNameWithoutExtension.conf")
    if (Files.exists(confFile)) propsFromFile(confFile)
    else Map.empty[String, String]
  }

  /**
    * @param params key-value pairs
    * @return key-value pairs where key https.keyStore, if any, is an absolute path
    */
  def conformize(params: Map[String, String]): Map[String, String] = {
    params.get(keyStoreKey).map { keyStorePath =>
      val absKeyStorePath = FileUtilities.pathTo(keyStorePath).toAbsolutePath
      params.updated(keyStoreKey, absKeyStorePath.toString)
    }.getOrElse {
      params
    }
  }

  private def propsFromFile(file: Path): Map[String, String] = {
    if (Files.exists(file)) Util.props(file)
    else Map.empty[String, String]
  }
}

object PlayLifeCycle {
  private val log = Logger(getClass)
}