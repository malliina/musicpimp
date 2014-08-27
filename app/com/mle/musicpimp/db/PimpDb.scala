package com.mle.musicpimp.db

import com.mle.db.DatabaseLike

import scala.slick.driver.H2Driver.simple._
import scala.slick.jdbc.GetResult

/**
 * @author Michael
 */
object PimpDb extends DatabaseLike {
  // To keep the content of an in-memory database as long as the virtual machine is alive, use
  // jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1
  override val database = Database.forURL("jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
  val tracks = TableQuery[Tracks]
  override val tableQueries = Seq(tracks)
  implicit val dataResult = GetResult(r => DataTrack(r.<<, r.<<, r.<<, r.<<))

  def fullText(query: String): Seq[DataTrack] =
    queryPlainParam[DataTrack, String]("SELECT T.* FROM FT_SEARCH_DATA(?,0,0) FT, TRACKS T WHERE FT.TABLE='TRACKS' AND T.ID=FT.KEYS[0];", query)
}
