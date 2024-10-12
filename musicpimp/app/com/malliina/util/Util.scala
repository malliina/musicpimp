package com.malliina.util

import java.net.URL
import java.nio.file.{Files, Path}
import com.malliina.exception.ResourceNotFoundException
import com.malliina.file.FileUtilities

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.io.{BufferedSource, Source}

/** Utility methods.
  */
object Util:

  def timed[T](task: => T): (T, FiniteDuration) =
    val start = System.currentTimeMillis()
    val t = task
    val duration = (System.currentTimeMillis() - start).millis
    (t, duration)

  /** try-with-resources Scala style
    *
    * @see
    *   [[com.malliina.util.Util]]#resource
    */
  def using[T <: AutoCloseable, U](resource: T)(op: T => U): U =
    try
      op(resource)
    finally
      resource.close()

  def addShutdownHook(code: => Unit): Unit =
    Runtime.getRuntime.addShutdownHook(
      new Thread:
        override def run(): Unit =
          code
    )

  def resource(resource: String): URL = obtainResource(resource, _.getResource)

  def resourceOpt(resource: String) = obtainResourceOpt(resource, _.getResource)

  def resourceUri(path: String) = resource(path).toURI

  def resourceUriOpt(path: String) = resourceOpt(path) map (_.toURI)

  def openStream(resource: String) = obtainResource(resource, _.getResourceAsStream)

  def obtainResource[T](resource: String, getter: ClassLoader => String => T): T =
    obtainResourceOpt(resource, getter)
      .getOrElse(throw new ResourceNotFoundException("Unable to locate resource: " + resource))

  def obtainResourceOpt[T](resource: String, getter: ClassLoader => String => T): Option[T] =
    Option(getter(getClass.getClassLoader)(resource))

  /** @return
    *   the uri of the file at the given path, or classpath resource if no classpath resource is
    *   found
    * @throws ResourceNotFoundException
    *   if neither a resource nor a file is found
    */
  def uri(path: String) =
    fileUriOpt(path) getOrElse resourceUri(path)

  def uriOpt(path: String) =
    fileUriOpt(path) orElse resourceUriOpt(path)

  def url(path: String) = uri(path).toURL

  def fileUriOpt(path: String) =
    val maybeFile = FileUtilities.pathTo(path)
    if Files.exists(maybeFile) then Some(maybeFile.toUri)
    else None

  /** Reads the properties of the classpath resource at the given path, or if none is found, of a
    * file at the given path.
    *
    * @return
    *   the properties as a map
    * @throws ResourceNotFoundException
    *   if neither a resource nor a file is found
    */
  def props(path: String) = Util.using(Source.fromURL(url(path)))(mappify)

  def props(path: Path) = Util.using(Source.fromFile(path.toUri))(mappify)

  def propsOpt(path: String) =
    uriOpt(path).map(r => Util.using(Source.fromURI(r))(mappify))

  private def mappify(src: BufferedSource) = src
    .getLines()
    .filter(line => line.contains("=") && !line.startsWith("#") && !line.startsWith("//"))
    .map(line => line.split("=", 2))
    .filter(_.length >= 2)
    .map(arr => arr(0) -> arr(1))
    .toMap

  /** Turns on SSL debug logging.
    *
    * Use for SSL troubleshooting.
    */
  def sslDebug(): Unit =
    sys.props("javax.net.debug") = "ssl"
