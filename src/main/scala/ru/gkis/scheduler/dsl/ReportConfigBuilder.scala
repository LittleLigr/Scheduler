package ru.gkis.scheduler.dsl

import ru.gkis.scheduler.BaseConfig
import ru.gkis.scheduler.model.meta.ReportMetaData
import ru.gkis.scheduler.model.raw.FcxConnection
import ru.gkis.scheduler.model.{ReportData, ReportFullData}

import java.time.LocalDateTime
import scala.util.{Failure, Try}

object ReportConfigBuilder {

    def apply(reportData: ReportData): Try[ReportFullData] = Try {
        val reportOrganization = reportData.parameters.find(param => param.paramName.equals("organization")) match {
            case Some(value) => value
            case _ => return Failure(new IllegalArgumentException())
        }

        val reportFcxFilePath = BaseConfig.config.getString("report.fcxConfigsPath")
        val reportFilePath = BaseConfig.config.getString("report.builtReportPath")
        val builderPath = BaseConfig.config.getString("fastReport.pathToBuilder")

        val reportConfigName = getReportConfigsName(reportData.reportHead.reportType, reportData.reportTask.id, reportOrganization.paramValue)
        val fcxFull = getReportFileFullPath(reportFcxFilePath, reportConfigName)
        val reportFileName = s"${reportData.reportHead.reportType}-$reportOrganization"

        val fcxConnection = FcxConnection(
            BaseConfig.config.getString("dwh.name"),
            BaseConfig.config.getString("dwh.adapter"),
            BaseConfig.config.getString("dwh.address"),
            BaseConfig.config.getInt("dwh.port"),
            BaseConfig.config.getString("dwh.database"),
            BaseConfig.config.getString("dwh.username"),
            BaseConfig.config.getString("dwh.password"),
            BaseConfig.config.getInt("dwh.timeout"),
            BaseConfig.config.getBoolean("dwh.pooling"),
            BaseConfig.config.getInt("dwh.minPoolSize"),
            BaseConfig.config.getInt("dwh.maxPoolSize"),
            BaseConfig.config.getInt("dwh.commandTimeout"),
            BaseConfig.config.getString("dwh.compatible"),
            BaseConfig.config.getString("dwh.krbsrvname")
        )

        val meta = ReportMetaData(fcxConnection, fcxFull, reportFileName, reportFilePath, sample = BaseConfig.config.getString("report.samplePath"), builderPath, LocalDateTime.now)
        ReportFullData(reportData, meta)
    }

    private def getReportConfigsName(reportType: String, reportId: Int, reportOrganization: String) = s"$reportType" +
        s"-$reportId" +
        s"-$reportOrganization" +
        s"-$reportType"

    private def getReportFileFullPath(reportConfigPath: String, reportConfigName: String, extension: String = "fcx") =
        s"$reportConfigPath\\$reportConfigName.$extension"
}
