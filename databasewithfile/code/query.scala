override def lookupSelectionWithDisplay(fundEngagementId: String): Future[Seq[FundEngagementReportTypeSelectionWithName]] = {
  val sql = for (((((((selection, fundEngagement), funds), fundAdmin), sheetReportMap), sheetTypes), reportTypes)
                   <- FundEngagementReportTypeSelection join FundEngagement on ((left, right) => left.fundEngagementId === right.id && left.fundEngagementId === fundEngagementId)
    join Funds on ((left, right) => left._2.fundid === right.id)
    join FundAdmin on ((left, right) => left._2.fundAdminId === right.id)
    join SheettypesReporttypesMap on (_._1._1._1.sheettypesReporttypesMapId === _.id)
    join SheetTypes on ((left, right) => left._2.sheettypeid === right.id)
    join ReportTypes on ((left, right) => left._1._2.reporttypeid === right.id))
    yield (selection.id, selection.fundEngagementId, selection.extractionStatus, selection.uploadFileStatus ,selection.uploadFileContent, selection.selected, sheetTypes.display, reportTypes.display,
      selection.createby, selection.createdatetime, selection.modifyby, selection.modifydatetime)
  db.run(sql.result).map(res => res.map(tup => FundEngagementReportTypeSelectionWithName(tup._1, tup._2, tup._3, tup._4, tup._5 match {
    case Some(value) => Some(new String(value.toString))
    case None => None
  }, tup._6, tup._7,tup._8, tup._9, tup._10, tup._11, tup._12)))
}