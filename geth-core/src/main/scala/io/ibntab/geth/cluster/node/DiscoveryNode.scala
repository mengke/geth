package io.ibntab.geth.cluster.node

import java.net.InetSocketAddress

import io.ibntab.geth.common.UUIDs
import io.ibntab.geth.common.network.NetworkAddress

/**
  *
  * @author ke.meng created on 2018/8/24
  */
class DiscoveryNode(_nodeName: String, val nodeId: String, val ephemeralId: String,
                    val hostName: String, val hostAddress: String, val address: InetSocketAddress) {

  def this(nodeId: String, address: InetSocketAddress) = this("", nodeId, address)

  def this(_nodeName: String, nodeId: String, address: InetSocketAddress) =
    this(_nodeName, nodeId, UUIDs.randomBase64UUID, address.getHostString, NetworkAddress.format(address.getAddress), address)

  val nodeName: String = if (_nodeName != null) _nodeName.intern() else ""

  def canEqual(other: Any): Boolean = other.isInstanceOf[DiscoveryNode]

  override def equals(other: Any): Boolean = other match {
    case that: DiscoveryNode =>
      (that canEqual this) &&
        ephemeralId == that.ephemeralId
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(ephemeralId)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }


  override def toString = s"DiscoveryNode($nodeName, $nodeId, $ephemeralId, $hostName, $hostAddress, $address)"
}
