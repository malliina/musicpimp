package controllers

import com.mle.musicpimp.db.DataTrack
import com.mle.util.Log
import play.api.mvc.{Action, Controller}

/**
 * @author Michael
 */
object Search extends Controller with Log {
  val testData = Seq(
    DataTrack("1", "Iron Maiden", "Powerslave", "Aces High"),
    DataTrack("2", "Pink", "Funhouse", "So What"),
    DataTrack("3", "Pendulum", "Immersion", "Under the Waves"),
    DataTrack("4", "Pendulum", "Immersion", "Witchcraft")
  )

  def search = Action(req => {
    val query = req.getQueryString("query").filter(_.nonEmpty)
    val results = query.fold(Seq.empty[String])(databaseSearch)
    Ok(views.html.search(query, results))
  })

  def databaseSearch(query: String): Seq[String] = List.fill(5)(query)
}


