package ru.gkis

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import org.junit.runner.RunWith
import org.scalatest.matchers.must.Matchers.not
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.junit.JUnitRunner
import ru.gkis.scheduler.actors.commands.{Command, Message, Shutdown}
import ru.gkis.scheduler.actors.{BuilderManager, EtlSynchronizer, ReportSender, TaskManager}
import ru.gkis.scheduler.model.meta.ReportMetaData
import ru.gkis.scheduler.model.raw.{FcxConnection, Reports, ReportsHistory}
import ru.gkis.scheduler.model.{ReportData, ReportFullData}
import ru.gkis.scheduler.{BaseConfig, Scheduler}

import java.io.File
import java.time.LocalDateTime

@RunWith(classOf[JUnitRunner])
class ActorTest extends AnyWordSpec {

    val testKit = ActorTestKit()

    "config test" in {
        BaseConfig.config should not be null
    }

    "actors spawn test" must {
        "spawn scheduler actor" in {
            val schedulerSystem = testKit.spawn(Scheduler())
        }
    }

    "shutdown test" must {
        "test graceful shutdown command" in {
            val schedulerSystem = testKit.spawn(Scheduler())
            schedulerSystem ! Shutdown
        }
    }

    "actors behaviors tests" must {

        val root = testKit.spawn(
            Behaviors.receiveMessage[Command] { message =>
                Behaviors.same
            }
        )

        val reportTask = ReportsHistory(1, 1, LocalDateTime.now(), Option(null), Option(null), "status")
        val report = Reports(1, "some type", "cron", "comment", true, Option(null), Option(null), "some export type")
        val testMessage = ReportData(reportTask, report, Seq(), Seq())

        val fcx = FcxConnection("name", "adapter", "address", 1, "database", "user", "password", 0, true, 0, 0, 0, "", "")
        val reportMeta = ReportMetaData(fcx, "some_test_fcx.fcx", "report file name", "report file path", "sample", "builder path", LocalDateTime.now())

        val fullData = ReportFullData(testMessage, reportMeta)

        "test etl synchronizer behavior" must {
            "test shutdown message incoming" in {
                val etl = testKit.spawn(EtlSynchronizer(root))
                etl ! Shutdown
            }

            "test some message incoming" in {
                val etl = testKit.spawn(EtlSynchronizer(root))
                etl ! Message(fullData)
            }

            "test empty message incoming" in {
                val etl = testKit.spawn(EtlSynchronizer(root))
                etl ! Message(null)
            }

            "test null incoming" in {
                val etl = testKit.spawn(EtlSynchronizer(root))
                etl ! null
            }
        }

        "test report sender behavior" must {
            "test shutdown message incoming" in {
                val reportSender = testKit.spawn(ReportSender(root))
                reportSender ! Shutdown
            }

            "test some message incoming" in {
                val reportSender = testKit.spawn(ReportSender(root))
                reportSender ! Message(fullData)
            }

            "test empty message incoming" in {
                val reportSender = testKit.spawn(ReportSender(root))
                reportSender ! Message(null)
            }

            "test null incoming" in {
                val reportSender = testKit.spawn(ReportSender(root))
                reportSender ! null
            }
        }

        "test task manager behavior" must {
            "test shutdown message incoming" in {
                val taskManager = testKit.spawn(TaskManager(root))
                taskManager ! Shutdown
            }

            "test some message incoming" in {
                val taskManager = testKit.spawn(TaskManager(root))
                taskManager ! Message(fullData)
            }

            "test empty message incoming" in {
                val taskManager = testKit.spawn(TaskManager(root))
                taskManager ! Message(null)
            }

            "test null incoming" in {
                val taskManager = testKit.spawn(TaskManager(root))
                taskManager ! null
            }
        }

        "test builder manager behavior" must {
            "test shutdown message incoming" in {
                val builderManager = testKit.spawn(BuilderManager(root))
                builderManager ! Shutdown
            }

            "test some message incoming" in {
                val builderManager = testKit.spawn(BuilderManager(root))
                builderManager ! Message(fullData)

                new File(fullData.reportMetaData.fcxFileName).exists() shouldBe true
            }

            "test empty message incoming" in {
                val builderManager = testKit.spawn(BuilderManager(root))
                builderManager ! Message(null)
            }

            "test null incoming" in {
                val builderManager = testKit.spawn(BuilderManager(root))
                builderManager ! null
            }
        }
    }

}
