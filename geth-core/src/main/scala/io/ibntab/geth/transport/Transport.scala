package io.ibntab.geth.transport

import java.io.IOException
import java.net.InetSocketAddress

import io.ibntab.geth.cluster.node.DiscoveryNode

/**
  *
  * @author ke.meng created on 2018/8/23
  */
trait Transport {

  def transportService: TransportService

  def boundAddress: InetSocketAddress

  def nodeConnected(node: DiscoveryNode): Boolean

  def connectToNode(node: DiscoveryNode, profile: ConnectionProfile,
                    connectionValidator: (DiscoveryNode, ConnectionProfile) => Option[IOException]): Unit

  def disconnectToNode(node: DiscoveryNode): Unit

  def newRequestId: Long

  def getConnection(node: DiscoveryNode): Connection

  def openConnection(node: DiscoveryNode, profile: ConnectionProfile): Connection

  trait Connection {

    def node: DiscoveryNode

    @throws[TransportException]
    @throws[IOException]
    def sendRequest(requestId: Long, action: String, request: TransportRequest, options: TransportRequestOptions): Unit

    def cacheKey: Object = this
  }
}
