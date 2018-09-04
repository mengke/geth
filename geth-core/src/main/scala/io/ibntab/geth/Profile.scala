package io.ibntab.geth

/**
  *
  * @author ke.meng created on 2018/8/22
  */
sealed abstract class Profile(val asJava: JavaProfile)

object Profile {

  case object Dev extends Profile(JavaProfile.DEV)
  case object Test extends Profile(JavaProfile.TEST)
  case object Beta extends Profile(JavaProfile.BETA)
  case object Prod extends Profile(JavaProfile.PROD)

  lazy val values: Set[Profile] = Set(Dev, Test, Beta, Prod)

}