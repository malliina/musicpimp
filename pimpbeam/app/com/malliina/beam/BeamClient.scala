package com.malliina.beam

import akka.actor.ActorRef
import com.malliina.values.Username

class BeamClient(val user: Username, val out: ActorRef)
