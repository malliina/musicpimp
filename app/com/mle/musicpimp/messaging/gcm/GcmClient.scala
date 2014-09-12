package com.mle.musicpimp.messaging.gcm

import com.mle.push.android.AndroidMessagingClient
import com.mle.push.gcm.GoogleMessaging

/**
 * What can a rogue actor do with the api key?
 *
 * @author mle
 */
object GcmClient extends GoogleMessaging(apiKey = "AIzaSyCCDniLRhlHAfnXIJnsVn-You2QQKLfrM8") with AndroidMessagingClient