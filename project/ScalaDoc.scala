package geth

import sbt._
import sbt.Keys._
import sbt.{AutoPlugin, Project, settingKey}

import sbtunidoc.BaseUnidocPlugin.autoImport.{ unidoc, unidocProjectFilter }
import sbtunidoc.JavaUnidocPlugin.autoImport.JavaUnidoc
import sbtunidoc.ScalaUnidocPlugin.autoImport.ScalaUnidoc
import sbtunidoc.GenJavadocPlugin.autoImport._

/**
  *
  * @author ke.meng created on 2018/8/21
  */
object ScalaDoc extends AutoPlugin {

}

object UnidocRoot extends AutoPlugin {

  object CliOptions {
    val genjavadocEnabled = CliOption("geth.genjavadoc.enabled", false)
  }

  object autoImport {
    val unidocRootIgnoreProjects = settingKey[Seq[Project]]("Projects to ignore when generating unidoc")
  }

  import autoImport._

  override def trigger = noTrigger
  override def requires =
    UnidocRoot.CliOptions.genjavadocEnabled.ifTrue(sbtunidoc.ScalaUnidocPlugin && sbtunidoc.JavaUnidocPlugin && sbtunidoc.GenJavadocPlugin)
      .getOrElse(sbtunidoc.ScalaUnidocPlugin)

  val gethSettings = UnidocRoot.CliOptions.genjavadocEnabled.ifTrue(
    Seq(javacOptions in (JavaUnidoc, unidoc) := Seq("-Xdoclint:none"))).getOrElse(Nil)

  override lazy val projectSettings = {
    def unidocRootProjectFilter(ignoreProjects: Seq[Project]) =
      ignoreProjects.foldLeft(inAnyProject) { _ -- inProjects(_) }

    inTask(unidoc)(Seq(
      unidocProjectFilter in ScalaUnidoc := unidocRootProjectFilter(unidocRootIgnoreProjects.value),
      unidocProjectFilter in JavaUnidoc := unidocRootProjectFilter(unidocRootIgnoreProjects.value),
      apiMappings in ScalaUnidoc := (apiMappings in (Compile, doc)).value))
  }
}

/**
  * Unidoc settings for every multi-project. Adds genjavadoc specific settings.
  */
object BootstrapGenjavadoc extends AutoPlugin {

  override def trigger = allRequirements
  override def requires = UnidocRoot.CliOptions.genjavadocEnabled.ifTrue(sbtunidoc.GenJavadocPlugin)
    .getOrElse(plugins.JvmPlugin)

  override lazy val projectSettings = UnidocRoot.CliOptions.genjavadocEnabled.ifTrue(
    Seq(
      unidocGenjavadocVersion := "0.11",
      scalacOptions in Compile ++= Seq("-P:genjavadoc:fabricateParams=true", "-P:genjavadoc:suppressSynthetic=false")
    )
  ).getOrElse(Nil)
}
