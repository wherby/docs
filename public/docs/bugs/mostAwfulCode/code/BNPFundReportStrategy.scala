import java.io.File
import scala.collection.Map

override def processFile(
                          file: File,
                          fileName: String,
                          fileType: FileType,
                          disabledAfterYEReports: Map[String, Boolean]
                        )(implicit
                          fundEngagement: FundEngagementData,
                          fund: Fund,
                          userEmail: String
                        ): Result = {
  fileType match {
    case contentType if contentType == FileType.XLSX || contentType == FileType.XLS =>
      processExcelFile(file.getPath, contentType.toString, disabledAfterYEReports)
    case _ =>
      encapErrorResponse(
        UnprocessableEntity,
        ExtractionErrorCode.UNSUPPORTED_REPORT_TYPE_FILE_EXTENSION
      )
  }
}

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
        // check fund name and worksheet name
        case ExcelMatchRule(name, rnameCell, None, (-1, -1), None, _) =>
          isReportNameMatch(name, sheet, rnameCell)
        // checks fund name but no checking on worksheet name
        case ExcelMatchRule(name, rnameCell, None, fundCell, None, _) =>
          if (isReportNameMatch(name, sheet, rnameCell))
            isFundNameMatch(fundName, sheet, fundCell, reportType)
          else false
        // check worksheet name but no checking on fund name
        case ExcelMatchRule(name, rnameCell, Some(sheetRegex), (-1, -1), None, _) =>
          isReportNameMatch(name, sheet, rnameCell) && regexMatch(sheetRegex, sheet.getSheetName)
        // check both fund name and worksheet name
        case ExcelMatchRule(name, rnameCell, Some(sheetRegex), fundCell, None, _) =>
          if (
            isReportNameMatch(name, sheet, rnameCell) && regexMatch(
              sheetRegex,
              sheet.getSheetName
            )
          )
            isFundNameMatch(fundName, sheet, fundCell, reportType)
          else false
        // check table header & sheet name
        case ExcelMatchRule(
        _,
        (-1, -1),
        Some(sheetRegex),
        (-1, -1),
        Some(headerRowRule),
        headerRowRange
        ) =>
          regexMatch(sheetRegex, sheet.getSheetName) && isHeaderRowMatch(
            sheet,
            headerRowRule,
            headerRowRange
          )
        // check table header
        case ExcelMatchRule(_, (-1, -1), None, (-1, -1), Some(headerRowRule), headerRowRange) =>
          isHeaderRowMatch(sheet, headerRowRule, headerRowRange)
        case _ => false
      }
  )
}