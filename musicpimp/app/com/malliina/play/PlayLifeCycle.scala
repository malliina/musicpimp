package com.malliina.play

import java.nio.file.{Files, Path, Paths}

import com.malliina.file.FileUtilities
import com.malliina.play.PlayLifeCycle.log
import com.malliina.security.KeyStores.{keyStoreKey, validateKeyStoreIfSpecified}
import com.malliina.util.Util
import play.api.Logger

/** Starts Play Framework 2, does not create a RUNNING_PID file.
  *
  * An alternative to the official ways to start Play, this integrates better with my init scripts.
  */
abstract class PlayLifeCycle(appName: String, registryPort: Int) {
  def start(): Unit = {
    // Init conf
    sys.props += ("pidfile.path" -> "/dev/null")
    FileUtilities.basePath = Paths get sys.props
      .get(s"$appName.home")
      .getOrElse(sys.props("user.dir"))
    log info s"Starting $appName... app home: ${FileUtilities.basePath}"
    sys.props ++= conformize(readConfFile(appName))
    validateKeyStoreIfSpecified()
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
    params
      .get(keyStoreKey)
      .map { keyStorePath =>
        val absKeyStorePath = FileUtilities.pathTo(keyStorePath).toAbsolutePath
        params.updated(keyStoreKey, absKeyStorePath.toString)
      }
      .getOrElse {
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
