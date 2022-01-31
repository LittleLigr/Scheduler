package ru.gkis.scheduler

import kamon.Kamon

object Metrics {

    val reportBuildError = Kamon.counter("app.report.error")
    val lastReportGauge = Kamon.gauge("app.report.last.id")
    val reportBuildCounter = Kamon.counter("app.report.count")
    val reportsInBuildQueue = Kamon.gauge("app.queue.reports")
    val reportBuildersQueue = Kamon.gauge("app.queue.builders")
}
