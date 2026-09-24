import com.malliina.rollup.DebPlugin
import com.typesafe.sbt.packager.Keys.*
import com.typesafe.sbt.packager.archetypes.TemplateWriter
import com.typesafe.sbt.packager.{Hashing, SettingsHelper, chmod}
import com.typesafe.sbt.packager.linux.{LinuxFileMetaData, LinuxPackageMapping, LinuxPlugin, LinuxSymlink}
import com.typesafe.sbt.packager.universal.Archives
import sbt.*
import sbt.Keys.*
import com.typesafe.sbt.packager.debian.DebianPlugin.autoImport.Debian
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.Universal

object FixedDebPlugin extends AutoPlugin:
  override def requires: Plugins = DebPlugin

  override def projectSettings: Seq[Setting[?]] = Seq(
    debianMaintainerScripts := Def.uncached:
      generateDebianMaintainerScripts(
        (Debian / maintainerScripts).value,
        (Debian / linuxScriptReplacements).value,
        (Universal / target).value
      )
  )

  private def generateDebianMaintainerScripts(
    scripts: Map[String, Seq[String]],
    replacements: Seq[(String, String)],
    tmpDir: File
  ): Seq[(File, String)] =
    scripts
      .map:
        case (scriptName, content) =>
          val scriptBits =
            TemplateWriter.generateScriptFromLines(content, replacements)
          val script = tmpDir / "tmp" / "debian" / scriptName
          IO.write(script, scriptBits mkString "\n")
          script -> scriptName
      .toList
