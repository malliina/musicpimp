package com.malliina.util

trait EnvUtils:

  sealed class OS(val name: String):
    // If this is not lazy, we get a StackOverFlowException, because Windows.name invokes
    // the constructor, but we would already be in the constructor.
    lazy val isUnixLike = name != Windows.name

  case object Windows extends OS("Windows")

  case object Mac extends OS("Mac")

  case object Linux extends OS("Linux")

  case object Solaris extends OS("Solaris")

  case object Unix extends OS("Unix")

  def operatingSystem: OS =
    val name = sys.props("os.name").toLowerCase
    def nameContains(s: String) = name.contains(s)
    if nameContains("win") then Windows
    else if nameContains("mac") then Mac
    else if nameContains("nux") then Linux
    else if nameContains("sunos") then Solaris
    else if nameContains("nix") then Unix
    else Unix

object EnvUtils extends EnvUtils
