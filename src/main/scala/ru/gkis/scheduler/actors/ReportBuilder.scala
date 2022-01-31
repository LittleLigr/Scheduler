package ru.gkis.scheduler.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import ru.gkis.scheduler.Metrics
import ru.gkis.scheduler.actors.commands._
import ru.gkis.scheduler.dsl.FcxConfig
import ru.gkis.scheduler.model.ReportFullData

import java.io.{File, FileWriter}
import scala.sys.process.Process
import scala.util.{Failure, Success, Try}

object ReportBuilder {

    def apply(manager: ActorRef[Command]): Behavior[Command] = Behaviors.setup[Command] { implicit context =>
        Behaviors.receiveMessage[Command] {
            case Message(report: ReportFullData) =>
                context.log.info(s"Building report task ${report.reportData.reportTask.id}")
                prepareReportFiles(report)
                val processResult = runFastReportBuilder(report)
                processResult match {
                    case Success(code) =>
                        manager ! BuilderDone(context.self, report)
                        Behaviors.same

                    case Failure(e) =>
                        context.log.error(s"Something gone wrong while building new report. This actor will be relaunched by BuilderManager\n$e")
                        Metrics.reportBuildError.withoutTags().increment()
                        throw e
                }

            case CoordinateShutdown(replyTo) =>
                replyTo ! ShutdownComplete(context.self)
                Behaviors.stopped
        }
    }

    private def runFastReportBuilder(report: ReportFullData)(implicit context: ActorContext[Command]): Try[Int] = {
        val processStartCommand = buildProcessCommand(report)
        context.log.info(processStartCommand)
        val process = Process(processStartCommand).run()
        process.exitValue() match {
            case 0 => Success(0)
            case _ => Failure(new IllegalArgumentException("FastReport builder return unallowed exit code"))
        }
    }

    private def buildProcessCommand(reportFullData: ReportFullData) =
        s""""${reportFullData.reportMetaData.builderPath}" "${reportFullData.reportMetaData.fcxFileName}""""

    private def prepareReportFiles(reportFullData: ReportFullData): Unit = {
        val fcxConfig = FcxConfig(reportFullData)
        val config = new File(reportFullData.reportMetaData.fcxFileName)
        config.createNewFile()

        val writer = new FileWriter(config)
        writer.write("""<?xml version="1.0" encoding="utf-8"?>\n""" + fcxConfig.toString)
        writer.close()
    }
}
