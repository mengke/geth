package io.ibntab.geth.common.io.stream

import java.io.{IOException, InputStream}
import java.nio.ByteBuffer

import io.ibntab.geth.common.bytes.BytesArray.EMPTY_BYTES
import io.ibntab.geth.common.bytes.{ByteString, BytesArray, BytesReference}

import scala.annotation.tailrec

/**
  *
  * @author ke.meng created on 2018/8/27
  */
abstract class StreamInput extends InputStream {

  @throws[IOException]
  def readByte(): Byte

  @throws[IOException]
  def readBytes(b: Array[Byte], offset: Int, len: Int): Int

  /**
    * Reads a bytes reference from this stream, might hold an actual reference to the underlying
    * bytes of the stream.
    */
  @throws[IOException]
  def readBytesReference(): BytesReference = {
    val len = readArraySize()
    readBytesReference(len)
  }

  /**
    * Reads an optional bytes reference from this stream. It might hold an actual reference to the underlying bytes of the stream. Use this
    * only if you must differentiate null from empty. Use {@link StreamInput#readBytesReference()} and
    * {@link StreamOutput#writeBytesReference(BytesReference)} if you do not.
    */
  @throws[IOException]
  def readOptionBytesReference(): Option[BytesReference] = {
    val len = readVInt() - 1
    if (len < 0) return None
    Some(readBytesReference(len))
  }

  /**
    * Reads a bytes reference from this stream, might hold an actual reference to the underlying
    * bytes of the stream.
    */
  @throws[IOException]
  def readBytesReference(len: Int): BytesReference = {
    if (len == 0) return BytesArray.EMPTY
    val bytes = new Array[Byte](len)
    readBytes(bytes, 0, len)
    new BytesArray(bytes, 0, len)
  }

  @throws[IOException]
  def readByteString(): ByteString = {
    val len = readArraySize()
    readByteString(len)
  }

  @throws[IOException]
  def readByteString(len: Int): ByteString = len match {
    case 0 => ByteString.empty
    case _ =>
      val bytes = new Array[Byte](len)
      readBytes(bytes, 0, len)
      ByteString(bytes)
  }

  @throws[IOException]
  def readFully(b: Array[Byte]): Unit = {
    readBytes(b, 0, b.length)
  }

  @throws[IOException]
  def readShort(): Short = (((readByte & 0xFF) << 8) | (readByte & 0xFF)).toShort

  @throws[IOException]
  def readInt(): Int = ((readByte & 0xFF) << 24) | ((readByte() & 0xFF) << 16) | ((readByte & 0xFF) << 8) | (readByte & 0xFF)

  @throws[IOException]
  def readVInt(): Int = {

    @tailrec def readVInt(i: Int, c: Int): Int = {
      val b = readByte()
      if ((b & 0x80) == 0)
        i
      else {
        if (c == 5) {
          throw new IOException("Invalid vInt ((" + Integer.toHexString(b) + " & 0x7f) << 28) | " + Integer.toHexString(i))
        } else {
          readVInt(i | (b & 0x7F) << (c * 7), c + 1)
        }
      }
    }
  }
}
