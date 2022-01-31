package ru.gkis.scheduler.model.raw

sealed trait ReportReceiverInterface

case class ReportMailDestination (mail: ReportsMails, address: Seq[ReportsMailsAdresses]) extends ReportReceiverInterface
case class ReportsTmp (id: Int, reportId: Int, path: String) extends ReportReceiverInterface
