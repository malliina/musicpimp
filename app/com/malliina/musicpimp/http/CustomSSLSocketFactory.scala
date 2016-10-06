package com.malliina.musicpimp.http

import java.net.{InetAddress, Socket}
import javax.net.ssl._

import com.malliina.security.SSLUtils

import scala.collection.JavaConversions.seqAsJavaList

object CustomSSLSocketFactory {
  def withSNI(sniMatcher: SNIMatcher, sniHost: SNIHostName): CustomSSLSocketFactory = {
    val sslParameters = new SSLParameters()
    sslParameters.setSNIMatchers(Seq(sniMatcher))
    sslParameters.setServerNames(Seq(sniHost))
    val ctx = SSLUtils.trustAllSslContext()
    val inner = ctx.getSocketFactory
    new CustomSSLSocketFactory(inner, sslParameters)
  }
}

/**
  * @param inner         wrapped SSL socket factory
  * @param sslParameters SSL parameters, such as SNI settings
  * @see http://javabreaks.blogspot.fi/2015/12/java-ssl-handshake-with-server-name.html
  */
class CustomSSLSocketFactory(inner: SSLSocketFactory, sslParameters: SSLParameters) extends SSLSocketFactory {
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

  override def createSocket(inetAddress: InetAddress, i: Int, inetAddress1: InetAddress, i1: Int): Socket =
    customized(inner.createSocket(inetAddress, i, inetAddress1, i1))

  private def customized(s: Socket): Socket = {
    val sslSocket = s.asInstanceOf[SSLSocket]
    sslSocket.setSSLParameters(sslParameters)
    sslSocket
  }
}
