package io.ibntab.geth

import scala.concurrent.Future

/**
  *
  * @author ke.meng created on 2018/8/21
  */
trait ShardLike[-Req, +Rep] extends (Req => Future[Rep]) {

  def shardId: ShardId
}

case class ShardId(region: String, shardId: Int)
