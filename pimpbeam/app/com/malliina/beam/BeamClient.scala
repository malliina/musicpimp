package com.malliina.beam

import org.apache.pekko.actor.ActorRef
import com.malliina.values.Username

class BeamClient(val user: Username, val out: ActorRef)
