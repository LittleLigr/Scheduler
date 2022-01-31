package ru.gkis.scheduler.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import ru.gkis.scheduler.actors.commands.{Command, Etl, Message, Response, Shutdown}
import ru.gkis.scheduler.model.ReportFullData

object EtlSynchronizer {

    def apply(scheduler: ActorRef[Command]): Behavior[Command] = Behaviors.setup { context =>

        val reportBuffer = collection.mutable.ListBuffer[ReportFullData]()

        Behaviors.receiveMessage {
            case Message(value: ReportFullData) =>
                if (etlReady) {
                    reportBuffer.foreach(report => scheduler ! Response(report, Etl))
                    reportBuffer.clear
                    scheduler ! Response(value, Etl)
                } else reportBuffer addOne value
                Behaviors.same

            case Shutdown => Behaviors.stopped
        }
    }

    private def etlReady = true
}
