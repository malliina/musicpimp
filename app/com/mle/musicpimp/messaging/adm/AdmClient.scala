package com.mle.musicpimp.messaging.adm

import com.mle.push.adm.AmazonMessaging
import com.mle.push.android.AndroidMessagingClient

/**
 * What can an attacker do with this information?
 *
 * @author Michael
 */
object AdmClient
  extends AmazonMessaging(
    clientID = "amzn1.application-oa2-client.08957c08d1754dc2bf963d7c265f6c4b",
    clientSecret = "65d6d9e6dffb7bc5e452fcfc84d5cb77e2ddc8f07592e944a2b2630d6653fdf4")
  with AndroidMessagingClient