package geth

import java.io.{FileInputStream, InputStreamReader}
import java.util.Properties

import sbt._
import sbt.Keys._

import scala.collection.breakOut

/**
  *
  * @author ke.meng created on 2018/8/21
  */
object GethBuild {

  val parallelExecutionByDefault = false

  lazy val buildSettings = Dependencies.Versions ++ Seq(
    organization := "io.ibntab",
    // use the same value as in the build scope, so it can be overriden by stampVersion
    version := (version in ThisBuild).value)

  lazy val rootSettings = UnidocRoot.gethSettings ++
    Formatting.formatSettings ++ Seq(
    parallelExecution in GlobalScope := System.getProperty("geth.parallelExecution", parallelExecutionByDefault.toString).toBoolean,
    version in ThisBuild := "0.0.1-SNAPSHOT"
  )

  val (mavenLocalResolver, mavenLocalResolverSettings) =
    System.getProperty("geth.build.M2Dir") match {
      case null ⇒ (Resolver.mavenLocal, Seq.empty)
      case path ⇒
        // Maven resolver settings
        def deliverPattern(outputPath: File): String =
          (outputPath / "[artifact]-[revision](-[classifier]).[ext]").absolutePath

        val resolver = Resolver.file("user-publish-m2-local", new File(path))
        (resolver, Seq(
          otherResolvers := resolver :: publishTo.value.toList,
          publishM2Configuration := Classpaths.publishConfig(
            publishMavenStyle.value,
            deliverPattern(crossTarget.value),
            if (isSnapshot.value) "integration" else "release",
            ivyConfigurations.value.map(c => ConfigRef(c.name)).toVector,
            artifacts = packagedArtifacts.value.toVector,
            resolverName = resolver.name,
            checksums = checksums.in(publishM2).value.toVector,
            logging = ivyLoggingLevel.value,
            overwrite = true)))
    }

  lazy val resolverSettings = {
    // should we be allowed to use artifacts published to the local maven repository
    if (System.getProperty("geth.build.useLocalMavenResolver", "false").toBoolean)
      Seq(resolvers += mavenLocalResolver)
    else Seq.empty
  } ++ {
    // should we be allowed to use artifacts from sonatype snapshots
    if (System.getProperty("geth.build.useSnapshotSonatypeResolver", "false").toBoolean)
      Seq(resolvers += Resolver.sonatypeRepo("snapshots"))
    else Seq.empty
  } ++ Seq(
    pomIncludeRepository := (_ ⇒ false) // do not leak internal repositories during staging
  )

  private def allWarnings: Boolean = System.getProperty("geth.allwarnings", "false").toBoolean

  final val DefaultScalacOptions = Seq("-encoding", "UTF-8", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint")

  // -XDignore.symbol.file suppresses sun.misc.Unsafe warnings
  final val DefaultJavacOptions = Seq("-encoding", "UTF-8", "-Xlint:unchecked", "-XDignore.symbol.file")

  lazy val defaultSettings = resolverSettings ++
    TestExtras.Filter.settings ++
    Seq[Setting[_]](
      // compile options
      scalacOptions in Compile ++= DefaultScalacOptions,
      scalacOptions in Compile ++= (
        if (scalaBinaryVersion.value == "2.11" || System.getProperty("java.version").startsWith("1."))
          Seq("-target:jvm-1.8", "-javabootclasspath", CrossJava.Keys.fullJavaHomes.value("8") + "/jre/lib/rt.jar")
        else
        // -release 8 is not enough, for some reason we need the 8 rt.jar explicitly #25330
          Seq("-release", "8", "-javabootclasspath", CrossJava.Keys.fullJavaHomes.value("8") + "/jre/lib/rt.jar")),
      scalacOptions in Compile ++= (if (allWarnings) Seq("-deprecation") else Nil),
      scalacOptions in Test := (scalacOptions in Test).value.filterNot(opt ⇒
        opt == "-Xlog-reflective-calls" || opt.contains("genjavadoc")),
      javacOptions in compile ++= DefaultJavacOptions ++ Seq("-source", "8", "-target", "8", "-bootclasspath", CrossJava.Keys.fullJavaHomes.value("8") + "/jre/lib/rt.jar"),
      javacOptions in test ++= DefaultJavacOptions ++ Seq("-source", "8", "-target", "8", "-bootclasspath", CrossJava.Keys.fullJavaHomes.value("8") + "/jre/lib/rt.jar"),
      javacOptions in compile ++= (if (allWarnings) Seq("-Xlint:deprecation") else Nil),
      javacOptions in doc ++= Seq(),

      crossVersion := CrossVersion.binary,

      ivyLoggingLevel in ThisBuild := UpdateLogging.Quiet,

      licenses := Seq(("Apache License, Version 2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
      homepage := Some(url("http://mengke.github.io/")),

      /**
        * Test settings
        */
      fork in Test := true,
      // default JVM config for tests
      javaOptions in Test ++= {
        val defaults = Seq(
          // ## core memory settings
          "-XX:+UseG1GC",
          // most tests actually don't really use _that_ much memory (>1g usually)
          // twice used (and then some) keeps G1GC happy - very few or to no full gcs
          "-Xms3g", "-Xmx3g",

          // ## extra memory/gc tuning
          // this breaks jstat, but could avoid costly syncs to disc see http://www.evanjones.ca/jvm-mmap-pause.html
          "-XX:+PerfDisableSharedMem",
          // tell G1GC that we would be really happy if all GC pauses could be kept below this as higher would
          // likely start causing test failures in timing tests
          "-XX:MaxGCPauseMillis=300",
          // nio direct memory limit for artery/aeron (probably)
          "-XX:MaxDirectMemorySize=256m",

          // faster random source
          "-Djava.security.egd=file:/dev/./urandom")

        if (sys.props.contains("geth.ci-server"))
          defaults ++ Seq("-XX:+PrintGCTimeStamps", "-XX:+PrintGCDetails")
        else
          defaults
      },

      // all system properties passed to sbt prefixed with "geth." will be passed on to the forked jvms as is
      javaOptions in Test := {
        val base = (javaOptions in Test).value
        val gethSysProps: Seq[String] =
          sys.props.filter(_._1.startsWith("geth"))
            .map { case (key, value) ⇒ s"-D$key=$value" }(breakOut)

        base ++ gethSysProps
      },

      // with forked tests the working directory is set to each module's home directory
      // rather than the Geth root, some tests depend on Geth root being working dir, so reset
      testGrouping in Test := {
        val original: Seq[Tests.Group] = (testGrouping in Test).value

        original.map { group ⇒
          group.runPolicy match {
            case Tests.SubProcess(forkOptions) ⇒
              group.copy(runPolicy = Tests.SubProcess(forkOptions.withWorkingDirectory(
                workingDirectory = Some(new File(System.getProperty("user.dir"))))))
            case _ ⇒ group
          }
        }
      },

      parallelExecution in Test := System.getProperty("geth.parallelExecution", parallelExecutionByDefault.toString).toBoolean,
      logBuffered in Test := System.getProperty("geth.logBufferedTests", "false").toBoolean,

      // show full stack traces and test case durations
      testOptions in Test += Tests.Argument("-oDF")) ++
      mavenLocalResolverSettings ++
      docLintingSettings ++
      CrossJava.crossJavaSettings

  lazy val docLintingSettings = Seq(
    javacOptions in compile ++= Seq("-Xdoclint:none"),
    javacOptions in test ++= Seq("-Xdoclint:none"),
    javacOptions in doc ++= Seq("-Xdoclint:none"))

  def loadSystemProperties(fileName: String): Unit = {
    import scala.collection.JavaConverters._
    val file = new File(fileName)
    if (file.exists()) {
      println("Loading system properties from file `" + fileName + "`")
      val in = new InputStreamReader(new FileInputStream(file), "UTF-8")
      val props = new Properties
      props.load(in)
      in.close()
      sys.props ++ props.asScala
    }
  }
}
