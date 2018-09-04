package geth

import sbt.Keys._
import sbt._

/**
  *
  * @author ke.meng created on 2018/8/21
  */
object VersionGenerator {

  val settings: Seq[Setting[_]] = inConfig(Compile)(Seq(
    resourceGenerators += generateVersion(resourceManaged, _ / "version.conf",
      """|geth.version = "%s"
         |"""),
    sourceGenerators += generateVersion(sourceManaged, _ / "geth" / "Version.scala",
      """|package geth
         |
         |object Version {
         |  val current: String = "%s"
         |}
         |""")))

  def generateVersion(dir: SettingKey[File], locate: File ⇒ File, template: String) = Def.task[Seq[File]] {
    val file = locate(dir.value)
    val content = template.stripMargin.format(version.value)
    if (!file.exists || IO.read(file) != content) IO.write(file, content)
    Seq(file)
  }

}
