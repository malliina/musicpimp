package com.mle.messaging.gcm

import com.mle.musicpimp.util.FileSet

/**
 *
 * @author mle
 */
object GoogleDevices extends AndroidDevices("gcm.json")

object AmazonDevices extends AndroidDevices("adm.json")

class AndroidDevices(fileName: String) extends FileSet[AndroidDevice](fileName) {
  override def id(elem: AndroidDevice): String = elem.id
}