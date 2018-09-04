package io.ibntab.geth.transport

import scala.concurrent.duration.Duration

/**
  *
  * @author ke.meng created on 2018/8/24
  */
abstract class TransportRequest extends TransportMessage {



}

case class TransportRequestOptions(timeout: Duration, compress: Boolean, category: TransportRequestOptions.Category)

object TransportRequestOptions {

  def newBuilder = new Builder

  def newBuilder(options: TransportRequestOptions) = new Builder(options)

  final class Builder {

    def this(options: TransportRequestOptions) = {
      this()
      _timeout = options.timeout
      _compress = options.compress
      _category = options.category
    }

    private var _timeout: Duration = _
    private var _compress: Boolean = _
    private var _category: Category = _

    def timeout(timeout: Duration): Builder = {
      _timeout = timeout
      this
    }
    def compress(compress: Boolean): Builder = {
      _compress = compress
      this
    }
    def category(category: Category): Builder = {
      _category = category
      this
    }

    def build() = TransportRequestOptions(_timeout, _compress, _category)
  }

  sealed trait Category
  case object Worker extends Category
  case object Reg extends Category
  case object State extends Category
  case object Ping extends Category
}