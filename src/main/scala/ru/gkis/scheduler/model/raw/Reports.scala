package ru.gkis.scheduler.model.raw

import java.time.LocalDateTime

case class Reports(id: Int, reportType: String, cron: String, comment: String, enable: Boolean, lastSynchronization: Option[LocalDateTime], synchronizationStatus: Option[String], exportType: String)
