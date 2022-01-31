package ru.gkis.scheduler.actors.commands

import akka.actor.typed.ActorRef
import ru.gkis.scheduler.model.ReportFullData

sealed trait Command


case class Message[T](value: T) extends Command

case class Response[T](value: T, step: BuildStep) extends Command

case class ReportComplete(reportFullData: ReportFullData) extends Command

case class BuilderDone(builder: ActorRef[Command], report: ReportFullData) extends Command

case object Shutdown extends Command

case class CoordinateShutdown(replyTo: ActorRef[Command]) extends Command

case class ShutdownComplete(ref: ActorRef[Command]) extends Command

case object SoulReaperShutdownComplete extends Command


sealed trait BuildStep

case object Record extends BuildStep

case object Task extends BuildStep

case object Etl extends BuildStep

case object Build extends BuildStep

case object Send extends BuildStep

