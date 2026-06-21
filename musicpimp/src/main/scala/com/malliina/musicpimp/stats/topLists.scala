package com.malliina.musicpimp.stats

import com.malliina.http.FullUrl
import com.malliina.musicpimp.http4s.Reverse
import io.circe.Codec
import org.http4s.Uri

abstract class ListLike[T <: TopEntry](val entries: Seq[T], baseCall: Uri):
  val prevOffset = math.max(0, meta.from - meta.maxItems)
  val nextOffset = meta.from + meta.maxItems

  def meta: DataRequest

  def username = meta.username

  def from = meta.from

  def until = meta.until

  def prev = withOffset(prevOffset)

  def next = withOffset(nextOffset)

  def withOffset(offset: Int) = baseCall.withQueryParam("from", offset)

case class PopularList(meta: DataRequest, populars: Seq[FullPopularEntry])
  extends ListLike(populars, Reverse.website.popular) derives Codec.AsObject

object PopularList:
  def forEntries(meta: DataRequest, populars: Seq[PopularEntry], host: FullUrl): PopularList =
    PopularList(meta, populars.map(_.toFull(host)))

case class RecentList(meta: DataRequest, recents: Seq[FullRecentEntry])
  extends ListLike(recents, Reverse.website.recent) derives Codec.AsObject

object RecentList:
  def forEntries(meta: DataRequest, recents: Seq[RecentEntry], host: FullUrl): RecentList =
    RecentList(meta, recents.map(_.toFull(host)))
