package geth

import sbt._
import sbt.Keys._

/**
  *
  * @author ke.meng created on 2018/8/21
  */
object Dependencies {

  lazy val scalaTestVersion = settingKey[String]("The version of ScalaTest to use.")
  lazy val scalaCheckVersion = settingKey[String]("The version of ScalaCheck to use.")
  val junitVersion = "4.12"
  val slf4jVersion = "1.7.25"
  val nettyVersion = "4.1.28.Final"
  val configVersion = "1.3.3"
  val guiceVersion = "4.2.0"
  val guavaVersion = "26.0-jre"
  val logbackVersion = "1.2.3"

  val Versions = Seq(
    crossScalaVersions := Seq("2.12.6", "2.11.12"),
    scalaVersion := System.getProperty("geth.build.scalaVersion", crossScalaVersions.value.head),
    scalaCheckVersion := sys.props.get("geth.build.scalaCheckVersion").getOrElse(
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 12 ⇒ "1.14.0" // does not work for 2.11
        case _                       ⇒ "1.13.2"
      }),
    scalaTestVersion := "3.0.5"
  )

  object Compile {

    val config = "com.typesafe" % "config" % configVersion // ApacheV2

    val nettyTransport = "io.netty" % "netty-transport" % nettyVersion // ApacheV2

    val slf4jApi = "org.slf4j" % "slf4j-api" % slf4jVersion // MIT

    val guice = "com.google.inject" % "guice" % guiceVersion // ApacheV2

    val lombok = "org.projectlombok" % "lombok" % "1.18.2" // MIT

    val guava = "com.google.guava" % "guava" % guavaVersion // ApacheV2
  }

  object Test {

    val junit = "junit" % "junit" % junitVersion % "test" // Common Public License 1.0

    val logback = "ch.qos.logback" % "logback-classic" % logbackVersion % "test" // EPL 1.0 / LGPL 2.1

    val mockito = "org.mockito" % "mockito-core" % "2.21.0" % "test" // MIT

    val scalatest = Def.setting { "org.scalatest" %% "scalatest" % scalaTestVersion.value % "test" } // ApacheV2

    val scalacheck = Def.setting { "org.scalacheck" %% "scalacheck" % scalaCheckVersion.value % "test" } // New BSD
  }

  import Compile._

  val l = libraryDependencies

  val core = l ++= Seq(config, nettyTransport, slf4jApi, guice, lombok)

  val coreTests = l ++= Seq(Test.junit, Test.scalatest.value, Test.mockito, Test.scalacheck.value)
}

object DependencyHelpers {

  case class ScalaVersionDependentModuleID(modules: String ⇒ Seq[ModuleID]) {
    def %(config: String): ScalaVersionDependentModuleID =
      ScalaVersionDependentModuleID(version ⇒ modules(version).map(_ % config))
  }
  object ScalaVersionDependentModuleID {
    implicit def liftConstantModule(mod: ModuleID): ScalaVersionDependentModuleID = versioned(_ ⇒ mod)

    def versioned(f: String ⇒ ModuleID): ScalaVersionDependentModuleID = ScalaVersionDependentModuleID(v ⇒ Seq(f(v)))
    def fromPF(f: PartialFunction[String, ModuleID]): ScalaVersionDependentModuleID =
      ScalaVersionDependentModuleID(version ⇒ if (f.isDefinedAt(version)) Seq(f(version)) else Nil)
  }

  /**
    * Use this as a dependency setting if the dependencies contain both static and Scala-version
    * dependent entries.
    */
  def versionDependentDeps(modules: ScalaVersionDependentModuleID*): Def.Setting[Seq[ModuleID]] =
    libraryDependencies ++= modules.flatMap(m ⇒ m.modules(scalaVersion.value))

  val ScalaVersion = """\d\.\d+\.\d+(?:-(?:M|RC)\d+)?""".r
  val nominalScalaVersion: String ⇒ String = {
    // matches:
    // 2.12.0-M1
    // 2.12.0-RC1
    // 2.12.0
    case version @ ScalaVersion() ⇒ version
    // transforms 2.12.0-custom-version to 2.12.0
    case version                  ⇒ version.takeWhile(_ != '-')
  }
}
