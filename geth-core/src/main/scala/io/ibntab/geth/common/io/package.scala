package io.ibntab.geth.common

import java.io.{IOException, InputStream}

import scala.annotation.tailrec

/**
  *
  * @author ke.meng created on 2018/8/27
  */
package object io {

  @inline
  def readFully(reader: InputStream, dest: Array[Byte]): Int = readFully(reader, dest, 0, dest.length)

  @throws[IOException]
  def readFully(reader: InputStream, dest: Array[Byte], offset: Int, len: Int): Int = {
    @tailrec
    def read(reader: InputStream, dest: Array[Byte], offset: Int, len: Int, readBytes: Int): Int = {
      val r = reader.read(dest, offset, len)
      if (r == -1) {
        readBytes
      } else {
        read(reader, dest, offset + r, len - r, readBytes + r)
      }
    }
    read(reader, dest, offset, len, 0)
  }
}
