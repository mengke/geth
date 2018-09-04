package io.ibntab.geth.common

import java.util.Base64

import scala.util.Random


/**
  *
  * @author ke.meng created on 2018/8/21
  */

sealed trait UUIDGenerator {
  def getBase64UUID: String
}

private[common] class RandomBasedUUIDGenerator extends UUIDGenerator {
  override def getBase64UUID: String = getBase64UUID(new Random(SecureRandomHolder.SecureRandom))

  def getBase64UUID(random: Random): String = {
    val randomBytes = new Array[Byte](16)
    random.nextBytes(randomBytes)

    /* Set the version to version 4 (see http://www.ietf.org/rfc/rfc4122.txt)
		 * The randomly or pseudo-randomly generated version.
		 * The version number is in the most significant 4 bits of the time
		 * stamp (bits 4 through 7 of the time_hi_and_version field).*/
    randomBytes(6) &= 0x0f /* clear the 4 most significant bits for the version  */
    randomBytes(6) |= 0x40 /* set the version to 0100 / 0x40 */

    /* Set the variant:
     * The high field of th clock sequence multiplexed with the variant.
     * We set only the MSB of the variant*/
    randomBytes(8) &= 0x3f /* clear the 2 most significant bits */
    randomBytes(8) |= 0x80 /* set the variant (MSB is set)*/

    Base64.getUrlEncoder.withoutPadding.encodeToString(randomBytes)
  }
}

object UUIDs {

  val RandomUUIDGenerator = new RandomBasedUUIDGenerator

  def randomBase64UUID: String = RandomUUIDGenerator.getBase64UUID
}
