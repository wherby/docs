
override def getExcelReportProcessorClass(
                                           sheet: Sheet,
                                           fundName: String,
                                           fundEngagementId: String
                                         ): Option[String] = {

  ruleMatchProcess(
    map,
    fundEngagementId,
    (rule: MatchRule, reportType: EGATab) =>
      rule match {
        // if processor rule does not check fund name and worksheet name
        case ExcelMatchRule(name, rnameCell, None, (-1, -1), None, _) =>
          isReportNameMatch(name, sheet, rnameCell)
        // if processor rule checks fund name but no checking on worksheet name
        case ExcelMatchRule(name, rnameCell, None, fundCell, None, _) =>
          if (isReportNameMatch(name, sheet, rnameCell))
            isFundNameMatch(fundName, sheet, fundCell, reportType)
          else false
        // if processor rule checks worksheet name but no checking on fund name
        case ExcelMatchRule(name, rnameCell, Some(sheetRegex), (-1, -1), None, _) =>
          isReportNameMatch(name, sheet, rnameCell) && regexMatch(sheetRegex, sheet.getSheetName)
        // if processor rules check both fund name and worksheet name
        case ExcelMatchRule(name, rnameCell, Some(sheetRegex), fundCell, None, _) =>
          if (
            isReportNameMatch(name, sheet, rnameCell) && regexMatch(
              sheetRegex,
              sheet.getSheetName
            )
          )
            isFundNameMatch(fundName, sheet, fundCell, reportType)
          else false
        // if processor rules check table header
        case ExcelMatchRule(_, (-1, -1), None, (-1, -1), Some(headerRowRule), headerRowRange) =>
          isHeaderRowMatch(sheet, headerRowRule, headerRowRange)
        case _ => false
      }
  )
}