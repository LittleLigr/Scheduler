package ru.gkis.scheduler.model


import ru.gkis.scheduler.model.raw.{ReportReceiverInterface, Reports, ReportsHistory, ReportsMails, ReportsMailsAdresses, ReportsParameters}


case class ReportData (reportTask: ReportsHistory, reportHead: Reports, parameters: Seq[ReportsParameters], reportDestinations: Seq[ReportReceiverInterface])

