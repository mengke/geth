package geth

import sbt.Keys.{packageBin, packageOptions}
import sbt.{Compile, Def, Package, PackageOption, Task}

/**
  *
  * @author ke.meng created on 2018/8/21
  */
object AutomaticModuleName  {
  private val AutomaticModuleName = "Automatic-Module-Name"

  def settings(name: String): Seq[Def.Setting[Task[Seq[PackageOption]]]] = Seq(
    packageOptions in (Compile, packageBin) += Package.ManifestAttributes(AutomaticModuleName → name)
  )
}