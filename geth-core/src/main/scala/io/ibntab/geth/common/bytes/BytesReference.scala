package io.ibntab.geth.common.bytes

import java.io.{IOException, ObjectOutputStream, OutputStream}

/**
  *
  * @author ke.meng created on 2018/8/28
  */
abstract class BytesReference extends (Int => Byte) {

  private var hash: Option[Int] = None

  def length: Int

  def slice(from: Int, len: Int): BytesReference

  @throws[IOException]
  def writeTo(os: OutputStream): Unit = {
    toByteString.writeToOutputStream(new ObjectOutputStream(os))
  }

  def toByteString: ByteString

  def ramBytesUsed: Long

  def canEqual(other: Any): Boolean = other.isInstanceOf[BytesReference]

  override def equals(other: Any): Boolean = other match {
    case that: BytesReference =>
      if (that.length != this.length) {
        false
      } else {
        that.toByteString equals this.toByteString
      }
    case _ => false
  }

  override def hashCode(): Int = hash match {
    case None =>
      hash = Some(toByteString.hashCode())
      hash.get
    case Some(_) => _
  }
}

object BytesReference {

  def toBytes(reference: BytesReference): Array[Byte] = {
    val bytes = new Array[Byte](reference.length)
    reference.toByteString.copyToArray(bytes, 0, reference.length)
    bytes
  }
}

final case class BytesArray(bytes: Array[Byte], offset: Int, length: Int) extends BytesReference {

  def this(bytes: Array[Byte]) = this(bytes, 0, bytes.length)

  override def apply(idx: Int): Byte = bytes(offset + idx)

  override def slice(from: Int, len: Int): BytesReference = {
    if (from < 0 || (from + len) > this.length)
      throw new IllegalArgumentException(s"can't slice a buffer with length [${this.length}], with slice parameters from [$from], length [$len]")
    new BytesArray(bytes, offset + from, len)
  }

  override def toByteString: ByteString = ByteString.fromArray(bytes, offset, length)

  override def ramBytesUsed: Long = bytes.length
}

object BytesArray {
  val EMPTY_BYTES = new Array[Byte](0)

  val EMPTY = new BytesArray(EMPTY_BYTES, 0, 0)
}
