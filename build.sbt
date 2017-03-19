lazy val root = PimpBuild.root
lazy val frontend = PimpBuild.musicpimpFrontend
lazy val musicpimp = PimpBuild.musicpimp
lazy val cloudFrontend = PimpBuild.pimpcloudFrontend
lazy val pimpcloud = PimpBuild.pimpcloud
lazy val shared = PimpBuild.shared
lazy val it = PimpBuild.it
lazy val cross = PimpBuild.cross
lazy val crossJvm = PimpBuild.crossJvm
lazy val crossJs = PimpBuild.crossJs

addCommandAlias("pimp", ";project musicpimp")
addCommandAlias("cloud", ";project pimpcloud")
addCommandAlias("it", ";project it")
