package com.malliina.security

import java.security.cert.X509Certificate

import javax.net.ssl.{SSLContext, TrustManager, X509TrustManager}

object SSLUtils {

  /** Builds and initializes an [[javax.net.ssl.SSLContext]] that
    * trusts all certificates. Use this with SSL-enabled clients
    * that speak to servers with self-signed certificates.
    *
    * @return an SSL context that trusts all certificates
    */
  def trustAllSslContext() = {
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, Array[TrustManager](trustAllTrustManager()), null)
    sslContext
  }

  def trustAllTrustManager() = new X509TrustManager() {
    override def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = {}

    override def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = {}

    override def getAcceptedIssuers: Array[X509Certificate] = null
  }
}
