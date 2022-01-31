package ru.gkis.scheduler.actors

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import courier.Defaults.executionContext
import courier.{Envelope, Mailer}
import ru.gkis.scheduler.BaseConfig
import ru.gkis.scheduler.actors.commands._
import ru.gkis.scheduler.model.ReportFullData
import ru.gkis.scheduler.model.raw.ReportMailDestination

import java.io.File
import javax.mail.internet.InternetAddress
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object ReportSender {

    def apply(scheduler: ActorRef[Command]): Behavior[Command] = Behaviors.setup { implicit context =>

        Behaviors.receiveMessage[Command] {
            case Message(report: ReportFullData) =>
                sendMail(report) match {
                    case Success(_) =>
                        scheduler ! ReportComplete(report)
                        Behaviors.same
                    case Failure(exception) =>
                        context.log.error(s"Alarm, report error in SenderActor $report")
                        throw exception
                }

            case Shutdown => Behaviors.stopped
        }
    }

    private def sendMail(report: ReportFullData)(implicit context: ActorContext[Command]) = Try {
        val sendTimeout = BaseConfig.config.getInt("mail.timeout")

        val mailer = getMailer match {
            case Success(value) => value
            case Failure(exception) =>
                context.log.error(s"Mailer catch an exception.")
                throw exception
        }
        val envelopes = getEnvelopes(report)

        context.log.info(mailer.toString)
        context.log.info(envelopes.toString)

        envelopes.foreach(mail => Await.result(mailer(mail), sendTimeout seconds))
    }

    private def getEnvelopes(reportFullData: ReportFullData) =
        reportFullData.reportData.reportDestinations.map {
            case mail: ReportMailDestination => getEnvelope(mail, new File(reportFullData.reportMetaData.reportFileName))
        }

    private def getEnvelope(destination: ReportMailDestination, report: File) = {
        val mailSender = BaseConfig.config.getString("mail.sender")

        val envelope = Envelope.from(new InternetAddress(mailSender))
        destination.address.foreach(address => envelope.cc(new InternetAddress(address.mailAdress)))
        envelope.content(courier.Multipart()
            .text(destination.mail.mailText)
            .attach(report))
        envelope.subject(destination.mail.mailTheme)

        envelope
    }

    private def getMailer: Try[Mailer] = Try {
        val mailAddress = BaseConfig.config.getString("mail.server")
        val mailPort = BaseConfig.config.getInt("mail.port")

        Mailer(mailAddress, mailPort)()
    }
}
