package ru.gkis.scheduler.dsl

import ru.gkis.scheduler.model.ReportFullData
import ru.gkis.scheduler.model.meta.ReportMetaData
import ru.gkis.scheduler.model.raw.{FcxConnection, Reports, ReportsParameters}

import scala.xml.{Attribute, Elem, Null, Text}

object FcxConfig {
    def apply(reportFullData: ReportFullData): Elem = {
        <Config>
            {buildReportNode(reportFullData.reportMetaData.sample)}
            {buildParametersNode(reportFullData.reportData.parameters)}
            {buildConnectionsNode(reportFullData.reportMetaData.fcxConnection)}
            {buildExportNode(reportFullData.reportData.reportHead)}
            {buildSaveNode(reportFullData.reportMetaData)}
        </Config>
    }

    private def buildParametersNode(parameters: Seq[ReportsParameters]): Elem = {
        def setParameters(elem: Elem, param: ReportsParameters, id: Int): Elem = elem %
            Attribute(None, s"Type$id", Text(param.paramType), Null) %
            Attribute(None, s"Value$id", Text(param.paramValue), Null) %
            Attribute(None, s"Name$id", Text(param.paramName), Null)

        parameters.foldLeft((parametersElem, 1))((elem, params) => (setParameters(elem._1, params, elem._2), elem._2 + 1))._1
    }

    private def buildReportNode(path: String): Elem = reportElem % Attribute(None, "Path", Text(path), Null)

    private def buildConnectionsNode(connection: FcxConnection): Elem = {
        val connectionString =
            s"PORT=${connection.port};" +
            s"KRBSRVNAME=${connection.krbsrvname};" +
            s"TIMEOUT=${connection.timeout};" +
            s"POOLING=${connection.pooling};" +
            s"MINPOOLSIZE=${connection.minPoolSize};" +
            s"MAXPOOLSIZE=${connection.maxPoolSize};" +
            s"COMMANDTIMEOUT=${connection.commandTimeout};" +
            s"COMPATIBLE=${connection.compatible};" +
            s"HOST=${connection.address};" +
            s"USER ID=${connection.user};" +
            s"PASSWORD=${connection.password};" +
            s"DATABASE=${connection.database}"

        connectionsElem %
            Attribute(None, "ConnectionString1", Text(connectionString), Null) %
            Attribute(None, "Type1", Text(connection.adapter), Null) %
            Attribute(None, "Name1", Text(connection.name), Null)
    }

    private def buildExportNode(report: Reports): Elem = exportElem % Attribute(None, "As", Text(report.exportType), Null)

    private def buildSaveNode(reportMetaData: ReportMetaData): Elem = saveElem %
        Attribute(None, "FileName", Text(""), Null) %
        Attribute(None, "Timestamp", Text("True"), Null) %
        Attribute(None, "Path", Text(reportMetaData.reportFilePath), Null) %
        Attribute(None, "To", Text("folder"), Null)

    private def parametersElem = <Parameters/>

    private def reportElem = <Report/>

    private def connectionsElem = <Connections/>

    private def exportElem = <Export/>

    private def saveElem = <Save/>

    private def openAfterElem = <OpenAfter/>
}
