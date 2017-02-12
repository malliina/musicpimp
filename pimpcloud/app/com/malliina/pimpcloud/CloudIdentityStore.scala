package com.malliina.pimpcloud

import com.malliina.pimpcloud.CloudIdentityStore.AlreadyExists

trait CloudIdentityStore {
  type CloudID = String

  /** Generates a new ID and stores it in the database.
    *
    * @return the ID or [[AlreadyExists]] if there's a collision
    */
  def generateAndSave(): Either[AlreadyExists, CloudID]

  def trySave(id: CloudID): Either[AlreadyExists, CloudID]

  def remove(id: CloudID): Unit

  /**
    *
    * @param id
    * @return true if `id` is available, false otherwise
    */
  def exists(id: CloudID): Boolean
}

object CloudIdentityStore {

  trait DataMessage

  case class AlreadyExists(id: String) extends DataMessage

}
