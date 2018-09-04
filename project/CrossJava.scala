package geth

import java.io.File

import sbt._

import scala.annotation.tailrec
import scala.collection.immutable.ListMap

/**
  *
  * @author ke.meng created on 2018/8/21
  */
object CrossJava {

  object Keys {
    val discoveredJavaHomes = settingKey[Map[String, File]]("Discovered Java home directories")
    val javaHomes = settingKey[Map[String, File]]("The user-defined additional Java home directories")
    val fullJavaHomes = settingKey[Map[String, File]]("Combines discoveredJavaHomes and custom javaHomes.")
  }

  import Keys._

  val crossJavaSettings = Seq(
    discoveredJavaHomes := CrossJava.discoverJavaHomes,
    javaHomes := ListMap.empty,
    fullJavaHomes := CrossJava.expandJavaHomes(discoveredJavaHomes.value ++ javaHomes.value),
  )

  def discoverJavaHomes: ListMap[String, File] = {
    ListMap(JavaDiscoverConfig.configs flatMap { _.javaHomes } sortWith versionOrder: _*)
  }

  def versionOrder(left: (_, File), right: (_, File)): Boolean =
    versionOrder(left._2.getName, right._2.getName)

  // Sort version strings, considering 1.8.0 < 1.8.0_45 < 1.8.0_121
  @tailrec
  def versionOrder(left: String, right: String): Boolean = {
    val Pattern = """.*?([0-9]+)(.*)""".r
    left match {
      case Pattern(leftNumber, leftRest) =>
        right match {
          case Pattern(rightNumber, rightRest) =>
            if (Integer.parseInt(leftNumber) < Integer.parseInt(rightNumber)) true
            else if (Integer.parseInt(leftNumber) > Integer.parseInt(rightNumber)) false
            else versionOrder(leftRest, rightRest)
          case _ =>
            false
        }
      case _ =>
        true
    }
  }

  sealed trait JavaDiscoverConf {
    def javaHomes: Vector[(String, File)]
  }

  object JavaDiscoverConfig {
    private val JavaHomeDir = """(java-|jdk-?)(1\.)?([0-9]+).*""".r

    class LinuxDiscoverConfig(base: File) extends JavaDiscoverConf {
      def javaHomes: Vector[(String, File)] =
        wrapNull(base.list())
          .collect {
            case dir@JavaHomeDir(_, m, n) => JavaVersion(nullBlank(m) + n).toString -> (base / dir)
          }
    }

    class MacOsDiscoverConfig extends JavaDiscoverConf {
      val base: File = file("/Library") / "Java" / "JavaVirtualMachines"

      def javaHomes: Vector[(String, File)] =
        wrapNull(base.list())
          .collect {
            case dir@JavaHomeDir(_, m, n) =>
              JavaVersion(nullBlank(m) + n).toString -> (base / dir / "Contents" / "Home")
          }
    }

    class WindowsDiscoverConfig extends JavaDiscoverConf {
      val base: File = file("C://Program Files/Java")

      def javaHomes: Vector[(String, File)] =
        wrapNull(base.list())
          .collect {
            case dir@JavaHomeDir(_, m, n) => JavaVersion(nullBlank(m) + n).toString -> (base / dir)
          }
    }

    class JavaHomeDiscoverConfig extends JavaDiscoverConf {
      def javaHomes: Vector[(String, File)] =
        sys.env.get("JAVA_HOME")
          .map(new java.io.File(_))
          .filter(_.exists())
          .flatMap { javaHome =>
            val base = javaHome.getParentFile
            javaHome.getName match {
              case dir@JavaHomeDir(_, m, n) => Some(JavaVersion(nullBlank(m) + n).toString -> (base / dir))
              case _ => None
            }
          }
          .toVector
    }

    val configs = Vector(
      new LinuxDiscoverConfig(file("/usr") / "java"),
      new LinuxDiscoverConfig(file("/usr") / "lib" / "jvm"),
      new MacOsDiscoverConfig,
      new WindowsDiscoverConfig,
      new JavaHomeDiscoverConfig,
    )
  }

  def nullBlank(s: String): String =
    if (s eq null) ""
    else s

  // expand Java versions to 1-20 to 1.x, and vice versa to accept both "1.8" and "8"
  private val oneDot = Map((1L to 20L).toVector flatMap { i =>
    Vector(Vector(i) -> Vector(1L, i), Vector(1L, i) -> Vector(i))
  }: _*)

  def expandJavaHomes(hs: Map[String, File]): Map[String, File] =
    hs flatMap {
      case (k, v) =>
        val jv = JavaVersion(k)
        if (oneDot.contains(jv.numbers))
          Vector(k -> v, jv.withNumbers(oneDot(jv.numbers)).toString -> v)
        else Vector(k -> v)
    }

  def wrapNull(a: Array[String]): Vector[String] =
    if (a eq null) Vector()
    else a.toVector
}
