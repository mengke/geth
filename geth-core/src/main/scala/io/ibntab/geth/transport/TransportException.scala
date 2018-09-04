package io.ibntab.geth.transport

import java.net.InetSocketAddress

import io.ibntab.geth.cluster.node.DiscoveryNode
import io.ibntab.geth.exception.GethException

/**
  *
  * @author ke.meng created on 2018/8/27
  */
class TransportException extends GethException {



}

class ConnectTransportException private (val node: DiscoveryNode, val address: InetSocketAddress, val action: String) extends TransportException {

}

class NodeNotConnectedException extends ConnectTransportException {

}
