package com.malliina.musicpimp.http

import com.malliina.security.SSLUtils
import javax.net.ssl.HostnameVerifier
import org.apache.http.conn.ssl.{NoopHostnameVerifier, SSLConnectionSocketFactory}

trait ApacheHttpHelper {
  /**
    *
    * @return a socket factory that trusts all server certificates
    */
  def allowAllCertificatesSocketFactory(hostnameVerifier: HostnameVerifier = NoopHostnameVerifier.INSTANCE) =
    new SSLConnectionSocketFactory(SSLUtils.trustAllSslContext(), hostnameVerifier)
}

object ApacheHttpHelper extends ApacheHttpHelper