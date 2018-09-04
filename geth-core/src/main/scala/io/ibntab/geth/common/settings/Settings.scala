package io.ibntab.geth.common.settings

import java.io.File
import java.util.Properties

import com.typesafe.config._
import com.typesafe.config.impl.ConfigImpl
import io.ibntab.geth.common.util.{GethIO, StringEscapeUtils}
import io.ibntab.geth.exception.GethException

import scala.collection.JavaConverters._
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.control.NonFatal

/**
  *
  * @author ke.meng created on 2018/8/22
  */
object Settings {

  val empty: Settings = Settings(ConfigFactory.empty())

  def load(
            classLoader: ClassLoader = Settings.getClass.getClassLoader,
            properties: Properties = System.getProperties,
            directSettings: Map[String, AnyRef] = Map.empty,
            allowMissingApplicationConf: Boolean): Settings = {
    try {
      val systemPropertyConfig = if (properties eq System.getProperties) {
        ConfigImpl.systemPropertiesAsConfig()
      } else {
        ConfigFactory.parseProperties(properties)
      }

      val directConfig: Config = ConfigFactory.parseMap(directSettings.asJava)

      val applicationConfig: Config = {
        def setting(key: String): Option[AnyRef] =
          directSettings.get(key).orElse(Option(properties.getProperty(key)))

        {
          setting("config.resource").map(resource => ConfigFactory.parseResources(classLoader, resource.toString))
        } orElse {
          setting("config.file").map(fileName => ConfigFactory.parseFileAnySyntax(new File(fileName.toString)))
        } getOrElse {
          val parseOptions = ConfigParseOptions.defaults
            .setClassLoader(classLoader)
            .setAllowMissing(allowMissingApplicationConf)
          ConfigFactory.defaultApplication(parseOptions)
        }
      }

      val referenceConfig: Config = ConfigFactory.defaultReference()

      // Combine all the config together into one big config
      val combinedConfig: Config = Seq(
        systemPropertyConfig,
        directConfig,
        applicationConfig,
        referenceConfig
      ).reduceLeft(_ withFallback _)

      // Resolve settings. Among other things, the `play.server.dir` setting defined in directConfig will
      // be substituted into the default settings in referenceConfig.
      val resolvedConfig = combinedConfig.resolve

      Settings(resolvedConfig)
    } catch {
      case e: ConfigException => throw settingError(e.getMessage, Option(e.origin), Some(e))
    }
  }

  private[settings] def settingError(message: String,
                                     origin: Option[ConfigOrigin] = None,
                                     e: Option[Throwable] = None): SettingsException = {
    /*
      The stable values here help us from putting a reference to a ConfigOrigin inside the anonymous ExceptionSource.
      This is necessary to keep the Exception serializable, because ConfigOrigin is not serializable.
     */
    val originLine = origin.map(_.lineNumber: java.lang.Integer).orNull
    val originSourceName = origin.map(_.filename).orNull
    val originUrlOpt = origin.flatMap(o => Option(o.url))
    new SettingsException("Configuration error", message, e.orNull) {
      def line = originLine
      def position = null
      def input = originUrlOpt.map(GethIO.readUrlAsString).orNull
      def sourceName = originSourceName
      override def toString = "Configuration error: " + getMessage
    }
  }

  private[Settings] def asScalaList[A](l: java.util.List[A]): Seq[A] = asScalaBufferConverter(l).asScala.toList
}

case class Settings(underlying: Config) {

  /**
    * Merge two configurations. The second configuration overrides the first configuration.
    * This is the opposite direction of `Config`'s `withFallback` method.
    */
  def ++(other: Settings): Settings = {
    Settings(other.underlying.withFallback(underlying))
  }

  /**
    * Reads a value from the underlying implementation.
    * If the value is not set this will return None, otherwise returns Some.
    *
    * Does not check neither for incorrect type nor null value, but catches and wraps the error.
    */
  private def readValue[T](path: String, v: => T): Option[T] = {
    try {
      if (underlying.hasPathOrNull(path)) Some(v) else None
    } catch {
      case NonFatal(e) => throw reportError(path, e.getMessage, Some(e))
    }

  }

  /**
    * Check if the given path exists.
    */
  def has(path: String): Boolean = underlying.hasPath(path)

  /**
    * Get the config at the given path.
    */
  def get[A](path: String)(implicit loader: SettingLoader[A]): A = {
    loader.load(underlying, path)
  }

  /**
    * Get the config at the given path and validate against a set of valid values.
    */
  def getAndValidate[A](path: String, values: Set[A])(implicit loader: SettingLoader[A]): A = {
    val value = get(path)
    if (!values(value)) {
      throw reportError(path, s"Incorrect value, one of (${values.mkString(", ")}) was expected.")
    }
    value
  }

  /**
    * Get a value that may either not exist or be null. Note that this is not generally considered idiomatic Config
    * usage. Instead you should define all config keys in a reference.conf file.
    */
  def getOptional[A](path: String)(implicit loader: SettingLoader[A]): Option[A] = {
    try {
      if (underlying.hasPath(path)) Some(get[A](path)) else None
    } catch {
      case NonFatal(e) => throw reportError(path, e.getMessage, Some(e))
    }
  }

  /**
    * Returns available keys.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val keys = configuration.keys
    * }}}
    *
    * @return the set of keys available in this configuration
    */
  def keys: Set[String] = underlying.entrySet.asScala.map(_.getKey).toSet

  /**
    * Returns sub-keys.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * val subKeys = configuration.subKeys
    * }}}
    *
    * @return the set of direct sub-keys available in this configuration
    */
  def subKeys: Set[String] = underlying.root().keySet().asScala.toSet

  /**
    * Returns every path as a set of key to value pairs, by recursively iterating through the
    * config objects.
    */
  def entrySet: Set[(String, ConfigValue)] = underlying.entrySet().asScala.map(e => e.getKey -> e.getValue).toSet

  /**
    * Creates a configuration error for a specific configuration key.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * throw configuration.reportError("engine.connectionUrl", "Cannot connect!")
    * }}}
    *
    * @param path the configuration key, related to this error
    * @param message the error message
    * @param e the related exception
    * @return a configuration exception
    */
  def reportError(path: String, message: String, e: Option[Throwable] = None): GethException = {
    val origin = Option(if (underlying.hasPath(path)) underlying.getValue(path).origin else underlying.root.origin)
    Settings.settingError(message, origin, e)
  }

  /**
    * Creates a configuration error for this configuration.
    *
    * For example:
    * {{{
    * val configuration = Configuration.load()
    * throw configuration.globalError("Missing configuration key: [yop.url]")
    * }}}
    *
    * @param message the error message
    * @param e the related exception
    * @return a configuration exception
    */
  def globalError(message: String, e: Option[Throwable] = None): GethException = {
    Settings.settingError(message, Option(underlying.root.origin), e)
  }
}

/**
  * A setting loader
  */
trait SettingLoader[A] { self =>
  def load(config: Config, path: String = ""): A
  def map[B](f: A => B): SettingLoader[B] = (config: Config, path: String) => {
    f(self.load(config, path))
  }
}

object SettingLoader {

  def apply[A](f: Config => String => A): SettingLoader[A] = new SettingLoader[A] {
    def load(config: Config, path: String): A = f(config)(path)
  }

  import scala.collection.JavaConverters._

  implicit val stringLoader: SettingLoader[String] = SettingLoader(_.getString)
  implicit val seqStringLoader: SettingLoader[Seq[String]] = SettingLoader(_.getStringList).map(_.asScala)

  implicit val intLoader: SettingLoader[Int] = SettingLoader(_.getInt)
  implicit val seqIntLoader: SettingLoader[Seq[Int]] = SettingLoader(_.getIntList).map(_.asScala.map(_.toInt))

  implicit val booleanLoader: SettingLoader[Boolean] = SettingLoader(_.getBoolean)
  implicit val seqBooleanLoader: SettingLoader[Seq[Boolean]] =
    SettingLoader(_.getBooleanList).map(_.asScala.map(_.booleanValue))

  implicit val finiteDurationLoader: SettingLoader[FiniteDuration] =
    SettingLoader(_.getDuration).map(javaDurationToScala)
  implicit val seqFiniteDurationLoader: SettingLoader[Seq[FiniteDuration]] =
    SettingLoader(_.getDurationList).map(_.asScala.map(javaDurationToScala))

  implicit val durationLoader: SettingLoader[Duration] = SettingLoader { config => path =>
    if (config.getIsNull(path)) Duration.Inf
    else if (config.getString(path) == "infinite") Duration.Inf
    else finiteDurationLoader.load(config, path)
  }
  // Note: this does not support null values but it added for convenience
  implicit val seqDurationLoader: SettingLoader[Seq[Duration]] =
    seqFiniteDurationLoader.map(identity[Seq[Duration]])

  implicit val doubleLoader: SettingLoader[Double] = SettingLoader(_.getDouble)
  implicit val seqDoubleLoader: SettingLoader[Seq[Double]] =
    SettingLoader(_.getDoubleList).map(_.asScala.map(_.doubleValue))

  implicit val numberLoader: SettingLoader[Number] = SettingLoader(_.getNumber)
  implicit val seqNumberLoader: SettingLoader[Seq[Number]] = SettingLoader(_.getNumberList).map(_.asScala)

  implicit val longLoader: SettingLoader[Long] = SettingLoader(_.getLong)
  implicit val seqLongLoader: SettingLoader[Seq[Long]] =
    SettingLoader(_.getLongList).map(_.asScala.map(_.longValue))

  implicit val bytesLoader: SettingLoader[ConfigMemorySize] = SettingLoader(_.getMemorySize)
  implicit val seqBytesLoader: SettingLoader[Seq[ConfigMemorySize]] = SettingLoader(_.getMemorySizeList).map(_.asScala)

  private val configLoader: SettingLoader[Config] = SettingLoader(_.getConfig)
  private val seqConfigLoader: SettingLoader[Seq[Config]] = SettingLoader(_.getConfigList).map(_.asScala)

  implicit val settingsLoader: SettingLoader[Settings] = configLoader.map(Settings(_))
  implicit val seqSettingsLoader: SettingLoader[Seq[Settings]] = seqConfigLoader.map(_.map(Settings(_)))

  private def javaDurationToScala(javaDuration: java.time.Duration): FiniteDuration =
    Duration.fromNanos(javaDuration.toNanos)

  /**
    * Loads a value, interpreting a null value as None and any other value as Some(value).
    */
  implicit def optionLoader[A](implicit valueLoader: SettingLoader[A]): SettingLoader[Option[A]] = new SettingLoader[Option[A]] {
    def load(config: Config, path: String): Option[A] = {
      if (config.getIsNull(path)) None else {
        val value = valueLoader.load(config, path)
        Some(value)
      }
    }
  }

  implicit def mapLoader[A](implicit valueLoader: SettingLoader[A]): SettingLoader[Map[String, A]] = new SettingLoader[Map[String, A]] {
    def load(config: Config, path: String): Map[String, A] = {
      val obj = config.getObject(path)
      val conf = obj.toConfig

      obj.keySet().asScala.map { key =>
        // quote and escape the key in case it contains dots or special characters
        val path = "\"" + StringEscapeUtils.escapeEcmaScript(key) + "\""
        key -> valueLoader.load(conf, path)
      }(scala.collection.breakOut)
    }
  }
}
