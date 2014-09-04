package com.mle.messaging.adm

import com.mle.messaging.AndroidMessagingClient

import scala.concurrent.Future

/**
 * @author Michael
 */
object AdmClient extends AmazonMessaging with AndroidMessagingClient {
  // What can an attacker do with this information?
  override val clientID = "amzn1.application-oa2-client.08957c08d1754dc2bf963d7c265f6c4b"
  override val clientSecret = "65d6d9e6dffb7bc5e452fcfc84d5cb77e2ddc8f07592e944a2b2630d6653fdf4"

  def accessToken: Future[AccessToken] = accessToken(clientID, clientSecret)
}