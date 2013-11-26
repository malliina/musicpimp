package com.mle.musicpimp.actor

import akka.util.Timeout
import scala.concurrent.duration._

/**
 * @author Michael
 */
object ActorImplicits {
  implicit val timeout = Timeout(36500 days)
}
