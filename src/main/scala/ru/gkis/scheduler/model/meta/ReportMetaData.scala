package ru.gkis.scheduler.model.meta

import ru.gkis.scheduler.model.raw.FcxConnection

import java.time.LocalDateTime

case class ReportMetaData(fcxConnection: FcxConnection, fcxFileName: String, reportFileName: String, reportFilePath: String, sample: String, builderPath: String, buildStartTime: LocalDateTime)
