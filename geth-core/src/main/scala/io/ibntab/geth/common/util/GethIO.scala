package io.ibntab.geth.common.util

import java.io.{ByteArrayOutputStream, Closeable, IOException, InputStream}
import java.net.URL

import io.ibntab.geth.Logger

import scala.io.Codec

/**
  *
  * @author ke.meng created on 2018/8/22
  */
private[geth] object GethIO {

  private val logger = Logger(this.getClass)

  /*
   * Read the given stream into a byte array.
   *
   * Closes the stream.
   */
  private def readStream(stream: InputStream): Array[Byte] = {
    try {
      val buffer = new Array[Byte](8192)
      var len = stream.read(buffer)
      val out = new ByteArrayOutputStream() // Doesn't need closing
      while (len != -1) {
        out.write(buffer, 0, len)
        len = stream.read(buffer)
      }
      out.toByteArray
    } finally closeQuietly(stream)
  }

  /**
    * Read the given stream into a String.
    *
    * Closes the stream.
    */
  def readStreamAsString(stream: InputStream)(implicit codec: Codec): String = {
    new String(readStream(stream), codec.name)
  }

  /**
    * Read the URL as a String.
    */
  def readUrlAsString(url: URL)(implicit codec: Codec): String = {
    readStreamAsString(url.openStream())
  }

  /**
    * Close the given closeable quietly.
    *
    * Logs any IOExceptions encountered.
    */
  def closeQuietly(closeable: Closeable): Unit = {
    try {
      if (closeable != null) {
        closeable.close()
      }
    } catch {
      case e: IOException => logger.warn("Error closing stream", e)
    }
  }
}
