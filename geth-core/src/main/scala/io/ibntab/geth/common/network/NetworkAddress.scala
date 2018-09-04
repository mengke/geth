package io.ibntab.geth.common.network

import java.net.{Inet6Address, InetAddress}

import com.google.common.net.InetAddresses

/**
  *
  * @author ke.meng created on 2018/8/24
  */
object NetworkAddress {

  def format(address: InetAddress): String = format(address, -1)

  def format(address: InetAddress, port: Int): String = {
    assert(address != null)

    val host = if (port <= 0 && address.isInstanceOf[Inet6Address]) {
      InetAddresses.toUriString(address)
    } else {
      InetAddresses.toAddrString(address)
    }

    val result = if (port <= 0) host else host + ":" + port
    result
  }

}
