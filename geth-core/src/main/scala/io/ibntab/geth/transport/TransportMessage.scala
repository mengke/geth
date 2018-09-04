package io.ibntab.geth.transport

import java.net.InetSocketAddress

/**
  *
  * @author ke.meng created on 2018/8/24
  */
abstract class TransportMessage {

  val remoteAddress: InetSocketAddress

}
