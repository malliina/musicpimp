package com.malliina.musicpimp.cloud

import java.net.{InetAddress, Socket, URI}
import java.util
import javax.net.ssl.*

object CustomSSLSocketFactory:
  def forUri(uri: URI) = forHost(uri.getHost)

  def forHost(host: String): CustomSSLSocketFactory =
    val sniHost = new SNIHostName(host)
    val matcher = SNIHostName.createSNIMatcher(host.replace(".", "\\."))
    withSNI(matcher, sniHost)

  def withSNI(sniMatcher: SNIMatcher, sniHost: SNIHostName): CustomSSLSocketFactory =
    val sslParameters = new SSLParameters()

    sslParameters.setSNIMatchers(util.Arrays.asList(sniMatcher))
    sslParameters.setServerNames(util.Arrays.asList(sniHost))
    val ctx = SSLContext.getDefault
    val inner = ctx.getSocketFactory
    new CustomSSLSocketFactory(inner, sslParameters)

/** @param inner
  *   wrapped SSL socket factory
  * @param sslParameters
  *   SSL parameters, such as SNI settings
  * @see
  *   http://javabreaks.blogspot.fi/2015/12/java-ssl-handshake-with-server-name.html
  */
class CustomSSLSocketFactory(inner: SSLSocketFactory, sslParameters: SSLParameters)
  extends SSLSocketFactory:
  override def getDefaultCipherSuites: Array[String] = inner.getDefaultCipherSuites

  override def getSupportedCipherSuites: Array[String] = inner.getSupportedCipherSuites

  override def createSocket(): Socket =
    customized(inner.createSocket())

  override def createSocket(socket: Socket, s: String, i: Int, b: Boolean): Socket =
    customized(inner.createSocket(socket, s, i, b))

  override def createSocket(s: String, i: Int): Socket =
    customized(inner.createSocket(s, i))

  override def createSocket(s: String, i: Int, inetAddress: InetAddress, i1: Int): Socket =
    customized(inner.createSocket(s, i, inetAddress, i1))

  override def createSocket(inetAddress: InetAddress, i: Int): Socket =
    customized(inner.createSocket(inetAddress, i))

  override def createSocket(
    inetAddress: InetAddress,
    i: Int,
    inetAddress1: InetAddress,
    i1: Int
  ): Socket =
    customized(inner.createSocket(inetAddress, i, inetAddress1, i1))

  private def customized(s: Socket): Socket =
    val sslSocket = s.asInstanceOf[SSLSocket]
    sslSocket.setSSLParameters(sslParameters)
    sslSocket
