package com.malliina.musicpimp.messaging

import com.malliina.push.wns.WNSCredentials

case class PushConf(apns: APNSCredentials,
                    gcmApiKey: String,
                    adm: ADMCredentials,
                    wns: WNSCredentials)
