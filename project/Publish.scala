package geth

import sbt.Keys._
import sbt._
import sbtwhitesource.WhiteSourcePlugin.autoImport.whitesourceIgnore

/**
  *
  * @author ke.meng created on 2018/8/21
  */
object Publish extends AutoPlugin {


}

/**
  * For projects that are not to be published.
  */
object NoPublish extends AutoPlugin {
  override def requires = plugins.JvmPlugin

  override def projectSettings = Seq(
    skip in publish := true,
    sources in (Compile, doc) := Seq.empty,
    whitesourceIgnore := true
  )
}
