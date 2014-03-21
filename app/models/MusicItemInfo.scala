package models


/**
 * @author Michael
 */
/**
 *
 * @param title
 * @param id
 * @param dir haxx
 */
abstract class MusicItemInfo(val title: String, val id: String, val dir: Boolean)

object MusicItemTypes extends Enumeration {
  type MusicItemType = Value
  val Song, Dir = Value
}