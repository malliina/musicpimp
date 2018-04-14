package com.malliina.musicpimp.messaging

import com.malliina.push.apns.APNSTokenConf
import com.malliina.push.wns.WNSCredentials

case class PushConf(apns: APNSTokenConf,
                    gcmApiKey: String,
                    adm: ADMCredentials,
                    wns: WNSCredentials)
