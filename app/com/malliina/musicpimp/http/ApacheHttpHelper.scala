package com.malliina.musicpimp.http

import javax.net.ssl.HostnameVerifier

import com.malliina.security.SSLUtils
import org.apache.http.conn.ssl.{NoopHostnameVerifier, SSLConnectionSocketFactory}

/**
 *
 * @author mle
 */
trait ApacheHttpHelper {
  /**
   *
   * @return a socket factory that trusts all server certificates
   */
  def allowAllCertificatesSocketFactory(hostnameVerifier: HostnameVerifier = NoopHostnameVerifier.INSTANCE) =
    new SSLConnectionSocketFactory(SSLUtils.trustAllSslContext(), hostnameVerifier)
}

object ApacheHttpHelper extends ApacheHttpHelper
