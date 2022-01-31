package ru.gkis.scheduler

import com.typesafe.config.{Config, ConfigFactory}

object BaseConfig {
    val config: Config = ConfigFactory.load
}
