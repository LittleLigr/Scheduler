package ru.gkis.scheduler.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import ru.gkis.scheduler.actors.commands.{Command, Shutdown, ShutdownComplete}

object SoulReaper {

    def apply(actors: Seq[ActorRef[Command]]): Behavior[Command] = Behaviors.setup[Command] { context =>
        context.log.info("Start Scheduler Soul reaper!")

        val actorsSouls = collection.mutable.ListBuffer[ActorRef[Command]]().addAll(actors)

        Behaviors.receiveMessage[Command] {
            case Shutdown =>
                context.log.info("Soul reaper get Shutdown command. Start killing actors")
                actors.foreach(actor => actor ! Shutdown)
                Behaviors.same

            case ShutdownComplete(ref) =>
                context.log.info("Job stopped: {}", ref.path.name)
                actorsSouls.subtractOne(ref)
                if (actorsSouls.isEmpty) {
                    context.log.info("Soul reaper successfully shutdown all actors")
                    Behaviors.stopped
                }
                else Behaviors.same
        }
    }

}