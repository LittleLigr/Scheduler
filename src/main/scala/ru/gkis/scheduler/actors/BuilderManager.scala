package ru.gkis.scheduler.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import ru.gkis.scheduler.actors.commands.{Shutdown, _}
import ru.gkis.scheduler.model.ReportFullData
import ru.gkis.scheduler.{BaseConfig, Metrics}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

object BuilderManager {

    private val buildersQueue = mutable.Queue[ActorRef[Command]]()
    private val aliveBuilders = mutable.ListBuffer[ActorRef[Command]]()
    private val reportQueue = mutable.Queue[ReportFullData]()

    def apply(scheduler: ActorRef[Command]): Behavior[Command] = Behaviors.setup[Command] { implicit context =>
        createBuilders match {
            case Success(value) =>
                aliveBuilders.addAll(value)
                buildersQueue.enqueueAll(value)
            case Failure(exception) =>
                context.log.error("Builder manager tried to create builders, but throw an exception.")
                throw exception
        }

        Behaviors.receiveMessage[Command] {
            case Message(report: ReportFullData) =>
                processMessage(report)
                Behaviors.same

            case BuilderDone(builder, report) =>
                if (reportQueue.isEmpty) addBuilderToQueue(builder)
                else processBuilder(builder)
                Metrics.lastReportGauge.withoutTags().update(report.reportData.reportHead.id)
                Metrics.reportBuildCounter.withoutTags().increment()
                scheduler ! Response(report, Build)
                Behaviors.same

            case Shutdown =>
                context.log.info("Builder manager will stop as fast as possible.")
                aliveBuilders.foreach(builder => builder ! CoordinateShutdown(context.self))
                Behaviors.same

            case ShutdownComplete(ref) =>
                aliveBuilders.subtractOne(ref)
                if (aliveBuilders.isEmpty) {
                    scheduler ! ShutdownComplete(context.self)
                    Behaviors.stopped
                } else Behaviors.same
        }
    }

    private def createBuilders(implicit context: ActorContext[Command]): Try[List[ActorRef[Command]]] = Try {
        val supervisedBuilder =
            Behaviors.supervise(ReportBuilder(context.self)).onFailure[IllegalArgumentException](SupervisorStrategy.restart)

        val builders = BaseConfig.config.getInt("scheduler.buildersCount")
        Range(1, builders).map(i => context.spawn(supervisedBuilder, s"reportBuilder$i")).toList
    }

    private def processBuilder(builder: ActorRef[Command]): Unit = {
        val report = getReportFromQueue
        builder ! Message(report)
    }

    private def processMessage(report: ReportFullData)(implicit context: ActorContext[Command]): Unit = {
        context.log.info(s"BuilderManager receive new task ${report.reportData.reportTask.id}!")
        if (buildersQueue.isEmpty) addReportToQueue(report)
        else getBuilderFromQueue ! Message(report)
    }

    private def addReportToQueue(report: ReportFullData): Unit = {
        reportQueue.enqueue(report)
        Metrics.reportsInBuildQueue.withoutTags().increment()
    }

    private def getReportFromQueue: ReportFullData = {
        Metrics.reportsInBuildQueue.withoutTags().decrement()
        reportQueue.dequeue()
    }

    private def addBuilderToQueue(builder: ActorRef[Command]): Unit = {
        buildersQueue.enqueue(builder)
        Metrics.reportsInBuildQueue.withoutTags().increment()
    }

    private def getBuilderFromQueue: ActorRef[Command] = {
        Metrics.reportsInBuildQueue.withoutTags().decrement()
        buildersQueue.dequeue()
    }


}