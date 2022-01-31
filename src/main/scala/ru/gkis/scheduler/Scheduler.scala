package ru.gkis.scheduler

import akka.actor.typed._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import kamon.Kamon
import ru.gkis.scheduler.actors._
import ru.gkis.scheduler.actors.commands._
import ru.gkis.scheduler.model.ReportFullData

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

object Scheduler extends App {

    Kamon.init()
    val schedulerSystem: ActorSystem[Command] = ActorSystem(Scheduler(), "scheduler")

    def apply(): Behavior[Command] = Behaviors.setup[Command] { implicit context =>
        val actors = createActors
        val taskManager = actors(Send)
        val soulReaper = context.spawn(SoulReaper(actors.values.toSeq), "soulReaper")

        Behaviors.receiveMessage[Command] {
            case response: Response[ReportFullData] =>
                actors(response.step) ! Message(response.value)
                taskManager ! response
                Behaviors.same

            case report: ReportComplete =>
                taskManager ! report
                Behaviors.same

            case Shutdown =>
                prepareSystemToShutdown(soulReaper)
                Behaviors.same

            case actorShutdown: ShutdownComplete =>
                soulReaper ! actorShutdown
                Behaviors.same

            case SoulReaperShutdownComplete => Behaviors.stopped
        }
    }

    private def prepareSystemToShutdown(soulReaper: ActorRef[Command])(implicit context: ActorContext[Command]): Unit = {
        context.log.info("Soul reaper start gracefulShutdown")
        soulReaper ! Shutdown
    }

    private def createActors(implicit context: ActorContext[Command]): Map[BuildStep, ActorRef[Command]] = {
        val supervisedSender = Behaviors.supervise(ReportSender(context.self)).onFailure[Exception](SupervisorStrategy.restart)
        val supervisedBuilder = Behaviors.supervise(BuilderManager(context.self)).onFailure[IllegalArgumentException](SupervisorStrategy.restart)
        val supervisedEtl = Behaviors.supervise(EtlSynchronizer(context.self)).onFailure[Exception](SupervisorStrategy.restart)
        val supervisedTaskManager = Behaviors.supervise(TaskManager(context.self)).onFailure[Exception](SupervisorStrategy.restart)

        val reportSender = context.spawn(supervisedSender, "reportSender")
        val builderManager = context.spawn(supervisedBuilder, "builderManager")
        val etlSynchronizer = context.spawn(supervisedEtl, "etlSynchronizer")
        val taskManager = context.spawn(supervisedTaskManager, "taskManager")

        Map(Task -> etlSynchronizer, Etl -> builderManager, Build -> reportSender, Send -> taskManager)
    }

    def gracefulShutdownWithTimeout(): Unit = {
        println("Start graceful shutdown with timeout.")
        implicit val executor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())
        val shutdownTimeout = BaseConfig.config.getInt("scheduler.shutdownTimeoutMS")

        Future {
            schedulerSystem ! Shutdown
            Kamon.stop()
            println(s"Killer wait $shutdownTimeout ms")
            Thread.sleep(shutdownTimeout)
        } onComplete { _ =>
            println("Time is up, terminating scheduler")
            schedulerSystem.terminate()
            System.exit(0)
        }
    }
}