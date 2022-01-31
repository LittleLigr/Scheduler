package ru.gkis.scheduler.model.raw

import java.time.LocalDateTime

case class ReportsHistory(id: Int, reportId: Int,taskCreate:LocalDateTime, buildStart: Option[LocalDateTime], buildEnd: Option[LocalDateTime], status: String)
