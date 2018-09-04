
enablePlugins(UnidocRoot, NoPublish, CopyrightHeader)

import geth._
import geth.GethBuild._

initialize := {
  // Load system properties from a file to make configuration from Jenkins easier
  loadSystemProperties("project/geth-build.properties")
  initialize.value
}

geth.GethBuild.buildSettings
shellPrompt := { s => Project.extract(s).currentProject.id + " > " }
resolverSettings

lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  core,
  coreTests
)

lazy val root = Project(
  id = "geth",
  base = file(".")
).aggregate(aggregatedProjects: _*)
  .settings(rootSettings: _*)
  .settings(unidocRootIgnoreProjects := Seq(coreTests))
  .settings(
    unmanagedSources in(Compile, headerCreate) := (baseDirectory.value / "project").**("*.scala").get
  )

lazy val core = gethModule("geth-core")
  .settings(Dependencies.core)
  .settings(AutomaticModuleName.settings("geth.core"))
  .settings(
    unmanagedSourceDirectories in Compile += {
      val ver = scalaVersion.value.take(4)
      (scalaSource in Compile).value.getParentFile / s"scala-$ver"
    }
  )
  .settings(VersionGenerator.settings)
  .enablePlugins(BoilerplatePlugin)

lazy val coreTests = gethModule("geth-core-tests")
  .dependsOn(core % "compile->compile;test->test")
  .settings(Dependencies.coreTests)
  .enablePlugins(NoPublish)

def gethModule(name: String): Project =
  Project(id = name, base = file(name))
    .settings(geth.GethBuild.buildSettings)
    .settings(geth.GethBuild.defaultSettings)
    .settings(geth.Formatting.formatSettings)
    .enablePlugins(BootstrapGenjavadoc)