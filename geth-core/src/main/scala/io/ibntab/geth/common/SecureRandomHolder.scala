package io.ibntab.geth.common

import java.security.SecureRandom

/**
  *
  * @author ke.meng created on 2018/8/24
  */
object SecureRandomHolder {

  val SecureRandom: SecureRandom = new SecureRandom()

}
