package io.ibntab.geth.transport

import java.util
import java.util.concurrent.atomic.AtomicInteger

import io.ibntab.geth.transport.TransportRequestOptions.Category

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration

/**
  *
  * @author ke.meng created on 2018/8/24
  */
final case class ConnectionProfile(handles: List[ConnectionCategoryHandle],
                                   numConnections: Int,
                                   connectTimeout: Duration, handshakeTimeout: Duration) {
  def numConnectionsPerCategory(category: TransportRequestOptions.Category): Int = {
    handles.find(category == _.category) match {
      case None => throw new AssertionError(s"no handle found for category: $category")
      case Some(h) => h.length
    }
  }
}

object ConnectionProfile {

  final class Builder {

    private val _handles: ListBuffer[ConnectionCategoryHandle] = ListBuffer.empty
    private val addedCategories = util.EnumSet.noneOf(classOf[TransportRequestOptions.Category])
    private var _offset = 0
    private var _connectTimeout: Duration = _
    private var _handshakeTimeout: Duration = _

    def this(source: ConnectionProfile) = {
      this()
      _handles ++ source.handles
      _offset = source.numConnections
      _handles.foreach(h => addedCategories.add(h.category))
      _connectTimeout = source.connectTimeout
      _handshakeTimeout = source.handshakeTimeout
    }

    def connectTimeout(connectTimeout: Duration): Builder = {
      if (connectTimeout.toMillis < 0) throw new IllegalArgumentException(s"connectTimeout must be non-negative but was: $connectTimeout")
      _connectTimeout = connectTimeout
      this
    }

    def handshakeTimeout(handshakeTimeout: Duration): Builder = {
      if (handshakeTimeout.toMillis < 0) throw new IllegalArgumentException(s"handshakeTimeout must be non-negative but was: $handshakeTimeout")
      _handshakeTimeout = handshakeTimeout
      this
    }

    def addConnections(numConnections: Int, category: Category): Builder = {
      if (category == null) throw new IllegalArgumentException("category must not be null")
      if (addedCategories.contains(category)) throw new IllegalArgumentException(s"category [$category] is already registered")
      addedCategories.add(category)
      _handles += ConnectionCategoryHandle(_offset, numConnections, category)
      _offset += numConnections
      this
    }

    def build(): ConnectionProfile = {
      val categories = util.EnumSet.allOf(classOf[TransportRequestOptions.Category])
      categories.removeAll(addedCategories)
      if (!categories.isEmpty) throw new IllegalStateException(s"not all types are added for this connection profile - missing categories: $categories")
      ConnectionProfile(_handles.toList, _offset, _connectTimeout, _handshakeTimeout)
    }
  }

}


final case class ConnectionCategoryHandle(offset: Int, length: Int, category: Category) {

  val counter = new AtomicInteger()

  def channel[T](channels: List[T]): T = {
    if (length == 0) throw new IllegalStateException(s"can't select channel size is 0 for category: $category")
    assert(channels.size >= offset + length, s"illegal size: ${channels.size}, expected >= ${offset + length}")
    channels(offset + Math.floorMod(counter.incrementAndGet, length))
  }
}
