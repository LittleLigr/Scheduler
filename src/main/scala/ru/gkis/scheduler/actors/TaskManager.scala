package ru.gkis.scheduler.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.typesafe.akka.extension.quartz.QuartzSchedulerTypedExtension
import ru.gkis.scheduler.actors.commands.{BuildStep, Command, Message, ReportComplete, Response, Send, Shutdown, Task}
import ru.gkis.scheduler.dsl.{Queries, ReportConfigBuilder}
import ru.gkis.scheduler.model.ReportFullData
import ru.gkis.scheduler.model.raw.Reports

import java.util.Date
import scala.util.{Failure, Success}

object TaskManager {

    def apply(scheduler: ActorRef[Command]): Behavior[Command] = Behaviors.setup[Command] { implicit context =>
        Queries.getAllEnableReports.foreach(report => createQuartzSchedule(report))

        Behaviors.receiveMessage[Command] {
            case Message(report: Reports) =>
                makeFullData(report) match {
                    case Success(value) =>
                        scheduler ! Response(value, Task)
                        Behaviors.same

                    case Failure(exception) =>
                        context.log.error("Task manager tried to get some information about report, but something gone wrong.")
                        throw exception
                }

            case ReportComplete(reportFullData) =>
                Queries.updateReportHistoryStatus(reportFullData.reportData.reportTask.id, Send)
                Behaviors.same

            case Response(report: ReportFullData, step: BuildStep) =>
                Queries.updateReportHistoryStatus(report.reportData.reportTask.id, Send)
                Behaviors.same

            case Shutdown =>
                QuartzSchedulerTypedExtension(context.system).shutdown(false)
                Behaviors.stopped
        }
    }

    private def makeFullData(report: Reports)(implicit context: ActorContext[Command]) = {
        context.log.info(s"TaskManager receive new task. Report $report")
        ReportConfigBuilder(Queries.getReportData(report))
    }

    private def createQuartzSchedule(report: Reports)(implicit context: ActorContext[Command]): Date = {
        QuartzSchedulerTypedExtension(context.system)
            .createTypedJobSchedule(
                report.reportType + report.id,
                context.self,
                msg = Message(report),
                cronExpression = report.cron)
    }
}
