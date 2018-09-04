package io.ibntab.geth.common.settings

import scala.collection.immutable
import scala.concurrent.duration.Duration
/**
  *
  * @author ke.meng created on 2018/8/22
  */
sealed trait Key {
  def matches(key: String): Boolean
}

class SimpleKey(val key: String) extends Key {
  override def matches(key: String): Boolean = this.key == key
}

class Setting[A](val key: Key, val fallbackSetting: Setting[A], val defaultValue: Settings => A, val validator: Validator[A]) {

  def this(key: Key, defaultValue: Settings => A, validator: Validator[A]) = this(key, null, defaultValue, validator)

  def this(key: String, defaultValue: Settings => A, validator: Validator[A]) = this(new SimpleKey(key), defaultValue, validator)

  def this(key: String, defaultValue: A, validator: Validator[A]) = this(key, Function.const(defaultValue), validator)

  def this(key: Key, defaultValue: Settings => A) = this(key, defaultValue, new EmptyValidator[A])

  def this(key: String, defaultValue: Settings => A) = this(new SimpleKey(key), defaultValue)

  def this(key: String, defaultValue: A) = this(key, Function.const(defaultValue))

  def this(key: Key, fallbackSetting: Setting[A]) = this(key, fallbackSetting, fallbackSetting.get, new EmptyValidator[A])

  def this(key: String, fallbackSetting: Setting[A]) = this(new SimpleKey(key), fallbackSetting)

  @inline
  def rawKey: String = key.toString

  @inline
  def exists(settings: Settings): Boolean = settings.has(rawKey)

  def get(settings: Settings): A = get(settings, validate = true)

  private def get(settings: Settings, validate: Boolean): A = {
    val value: A = getRaw(settings)
    if (validate) {
      val it = validator.settings
      val map: Map[Setting[A], A] = it.map(s => (s, s.get(settings, validate = false))).toMap
      validator.validate(value, map)
    }
    value
  }

  def getRaw(settings: Settings): A = {
    if (exists(settings)) {
      settings.get(rawKey)
    } else {
      defaultValue(settings)
    }
  }

  def matches(toTest: String): Boolean = key.matches(toTest)
}

object Setting {

  def intSetting(key: String, defaultValue: Int, minValue: Int, maxValue: Int) = new Setting[Int](key, defaultValue, new Validator[Int] {
    override def validate(value: Int, settings: Map[Setting[Int], Int]): Option[IllegalArgumentException] = validateInt(key, value, minValue, maxValue)
  })

  def intSetting(key: String, defaultValue: Int, minValue: Int) = new Setting[Int](key, defaultValue, new Validator[Int] {
    override def validate(value: Int, settings: Map[Setting[Int], Int]): Option[IllegalArgumentException] = validateInt(key, value, minValue)
  })

  def intSetting(key: String, fallbackSetting: Setting[Int], minValue: Int) =
    new Setting[Int](new SimpleKey(key), fallbackSetting, fallbackSetting get _, (v, _) => validateInt(key, v, minValue))

  def intSetting(key: String, defaultValue: Int): Setting[Int] = intSetting(key, defaultValue, Int.MinValue)

  def longSetting(key: String, defaultValue: Long, minValue: Long) =
    new Setting[Long](key, defaultValue, (v, _) => validateLong(key, v, minValue))

  def simpleString(key: String) = new Setting[String](key, "")

  def simpleString(key: String, fallbackSetting: Setting[String]) = new Setting[String](key, fallbackSetting)

  def simpleString(key: String, validator: Validator[String]) = new Setting[String](new SimpleKey(key), null, _ => "", validator)

  def boolSetting(key: String, defaultValue: Boolean) = new Setting[Boolean](key, defaultValue)

  def boolSetting(key: String, fallbackSetting: Setting[Boolean]) = new Setting[Boolean](key, fallbackSetting)

  def boolSetting(key: String, defaultValue: Settings => Boolean) = new Setting[Boolean](key, defaultValue)

  def doubleSetting(key: String, defaultValue: Double, minValue: Double) =
    new Setting[Double](key, defaultValue, (v, _) => validateDouble(key, v, minValue))

  def doubleSetting(key: String, defaultValue: Double) = new Setting[Double](key, defaultValue)

  def durationSetting(key: String, defaultValue: Settings => Duration, minValue: Duration) =
    new Setting[Duration](key, defaultValue, (v, _) => validateDuration(key, v, minValue))

  def durationSetting(key: String, defaultValue: Duration) = new Setting[Duration](key, defaultValue)

  def durationSetting(key: String, fallbackSetting: Setting[Duration]) = new Setting[Duration](key, fallbackSetting)

  def positiveDurationSetting(key: String, defaultValue: Duration): Setting[Duration] =
    durationSetting(key, _ => defaultValue, Duration.Zero)

  def seqSetting[B](key: String, defaultValue: Seq[B]): Setting[Seq[B]] = new Setting[Seq[B]](key, defaultValue)

  def seqSetting[B](key: String, fallbackSetting: Setting[Seq[B]]): Setting[Seq[B]] = new Setting[Seq[B]](key, fallbackSetting)

  def seqSetting[B](key: String, defaultValue: Settings => Seq[B]) = new Setting[Seq[B]](key, defaultValue)

  def groupSetting(key: String): Setting[Settings] = groupSetting(key, new EmptyValidator[Settings])

  def groupSetting(key: String, validator: Validator[Settings]) = new Setting[Settings](new SimpleKey(key), null, _ => Settings.empty, validator)

  private def validateInt(key: String, value: Int, minValue: Int, maxValue: Int): Option[IllegalArgumentException] = {
    if (value < minValue) {
      Some(new IllegalArgumentException(s"Failed to validate value [$value] for setting [$key] must be >= $minValue"))
    } else if (value > maxValue) {
      Some(new IllegalArgumentException(s"Failed to validate value [$value] for setting [$key] must be <= $maxValue"))
    } else {
      None
    }
  }

  private def validateInt(key: String, value: Int, minValue: Int): Option[IllegalArgumentException] = validateInt(key, value, minValue, Int.MaxValue)

  private def validateLong(key: String, value: Long, minValue: Long): Option[IllegalArgumentException] = {
    if (value < minValue) {
      Some(new IllegalArgumentException(s"Failed to validate value [$value] for setting [$key] must be >= $minValue"))
    } else {
      None
    }
  }

  private def validateDouble(key: String, value: Double, minValue: Double): Option[IllegalArgumentException] = {
    if (value < minValue) {
      Some(new IllegalArgumentException(s"Failed to validate value [$value] for setting [$key] must be >= $minValue"))
    } else {
      None
    }
  }

  private def validateDuration(key: String, value: Duration, minValue: Duration): Option[IllegalArgumentException] = {
    if (value < minValue) {
      Some(new IllegalArgumentException(s"Failed to validate value [$value] for setting [$key] must be >= $minValue"))
    } else {
      None
    }
  }
}

trait Validator[A] {
  /**
    * The validation routine for this validator.
    *
    * @param value    the value of this setting
    * @param settings a map from the settings specified by { @link #settings()}} to their values
    */
  def validate(value: A, settings: immutable.Map[Setting[A], A]): Option[IllegalArgumentException]

  /**
    * The settings needed by this validator.
    *
    * @return the settings needed to validate; these can be used for cross-settings validation
    */
  def settings: Iterator[Setting[A]] = Iterator.empty
}

class EmptyValidator[A] extends Validator[A] {
  override def validate(value: A, settings: Map[Setting[A], A]): Option[IllegalArgumentException] = None
}
