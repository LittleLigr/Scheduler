package ru.gkis.scheduler.dsl

import io.getquill.{Ord, PostgresJdbcContext, SnakeCase}
import ru.gkis.scheduler.BaseConfig
import ru.gkis.scheduler.actors.commands._
import ru.gkis.scheduler.model.ReportData
import ru.gkis.scheduler.model.raw._

import java.time.LocalDateTime
import java.util.Date

object Queries {

    private val reportInWorkTogether = BaseConfig.config.getInt("scheduler.buildersCount")

    private val ctx = new PostgresJdbcContext(SnakeCase, "ctx")

    import ctx._

    private implicit val encodeLocalDateTime = MappedEncoding[Date, Option[LocalDateTime]](localDateTimeToDate)
    private implicit val decodeLocalDateTime = MappedEncoding[Option[LocalDateTime], Date](dateToLocalDateTime)

    def getAllEnableReports: Seq[Reports] =
        ctx.run(query[Reports].filter(report => report.enable))

    def getReports: Seq[Reports] = ctx.run(query[Reports])

    def getReportByReportId(reportId: Int): Option[Reports] =
        ctx.run(quote(query[Reports].filter(report => report.id == lift(reportId)))).lastOption

    def getNewTasks: Seq[ReportsHistory] =
        ctx.run(quote(query[ReportsHistory]
            .filter(task => task.status.equals(lift(Task.toString)))
            .sortBy(report => report.taskCreate)(Ord.asc)
            .take(lift(reportInWorkTogether))))

    def updateReportSynchronization(reportId: Int, localDateTime: LocalDateTime) =
        ctx.run(query[Reports]
            .filter(_.id == lift(reportId))
            .update(_.lastSynchronization -> lift(Option(localDateTime))))

    def updateReportHistoryStatus(taskId: Int, step: BuildStep) = {
        ctx.run(query[ReportsHistory].filter(_.id == lift(taskId)).update(_.status -> lift(s"$step")))
        step match {
            case Build => ctx.run(query[ReportsHistory].filter(_.id == lift(taskId)).update(_.buildStart -> lift(Option(LocalDateTime.now()))))
            case Send => ctx.run(query[ReportsHistory].filter(_.id == lift(taskId)).update(_.buildEnd -> lift(Option(LocalDateTime.now()))))
            case _ =>
        }
    }

    def updateReportHistoryBuildEnd(taskId: Int, buildEnd: LocalDateTime) =
        ctx.run(query[ReportsHistory].filter(_.id == lift(taskId)).update(_.buildEnd -> lift(Option(buildEnd))))

    def updateReportHistoryBuildStart(taskId: Int, buildStart: Option[LocalDateTime]) =
        ctx.run(query[ReportsHistory].filter(_.id == lift(taskId)).update(_.buildStart -> lift(buildStart)))

    def getReportData(report: Reports): ReportData = {
        val params = getReportParameters(report.id)

        val mailOption = getReportMail(report.id)
        val mailDestination = mailOption match {
            case Some(mail) =>
                val mailAddress = getReportMailAddresses(mail.id)
                Seq[ReportReceiverInterface](ReportMailDestination(mail, mailAddress))
            case None => Seq[ReportReceiverInterface]()
        }

        val reportTemps = getReportTemplates(report.id)
        val reportsHistoryOption = createReportEvent(report.id)

        reportsHistoryOption match {
            case Some(history) => ReportData(history, report, params, mailDestination ++ reportTemps)
            case None => throw new IllegalArgumentException(s"Error. postgres create report task and return wrong id. Report id is ${report.id}")
        }
    }

    def getReportParameters(reportId: Int): Seq[ReportsParameters] =
        ctx.run(quote(query[ReportsParameters].filter(params => params.reportId == lift(reportId))))

    def getReportMail(reportId: Int): Option[ReportsMails] =
        ctx.run(quote(query[ReportsMails].filter(mail => mail.reportId == lift(reportId)))).lastOption

    def getReportMailAddresses(mailId: Int): Seq[ReportsMailsAdresses] =
        ctx.run(quote(query[ReportsMailsAdresses].filter(address => address.mailId == lift(mailId))))

    def getReportTemplates(reportId: Int): Seq[ReportsTmp] =
        ctx.run(query[ReportsTmp].filter(tmp => tmp.reportId == lift(reportId)))

    def createReportEvent(reportId: Int): Option[ReportsHistory] = {
        val taskId = ctx.run(quote(query[ReportsHistory].insert(
            _.reportId -> lift(reportId),
            _.taskCreate -> lift(LocalDateTime.now),
            _.status -> lift(Record.toString)
        )).returningGenerated(_.id))

        ctx.run(query[ReportsHistory].filter(rh => rh.id == lift(taskId))).lastOption
    }

    import java.time.{LocalDateTime, ZoneId}

    private def localDateTimeToDate(dateToConvert: Date): Option[LocalDateTime] = Option {
        if (dateToConvert == null) null
        else dateToConvert.toInstant.atZone(ZoneId.systemDefault).toLocalDateTime
    }

    private def dateToLocalDateTime(dateToConvert: Option[LocalDateTime]): Date = {
        dateToConvert match {
            case Some(value) => Date.from(value.atZone(ZoneId.systemDefault).toInstant)
            case None => null
        }
    }
}