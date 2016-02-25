package com.malliina.musicpimp.stats

import com.malliina.musicpimp.db.PimpSchema

class DatabaseStats {
  val plays = PimpSchema.plays
  val tracks = PimpSchema.tracks
  val users = PimpSchema.usersTable
}
