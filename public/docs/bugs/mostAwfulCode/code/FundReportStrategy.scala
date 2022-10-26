import java.io.File
import scala.collection.Map

class FundReportStrategyBase @Inject() (
                                         doraServer: DoraServer,
                                         securityItemRead: SecurityItemRead,
                                         fundEngagementReportTypeSelectionRead: FundEngagementReportTypeSelectionRead,
                                         fundEngagementReportTypeSelectionWrite: FundEngagementReportTypeSelectionWrite
                                       ) extends FundReportStrategy {

  var disabledAfterYEReports: Map[String, Boolean] = Map[String, Boolean]()

  override def processFundReport(
                                  request: Request[MultipartFormData[Files.TemporaryFile]],
                                  disabledAfterYEReports: Map[String, Boolean]
                                )(implicit fundEngagement: FundEngagementData, fund: Fund): Result = {
    implicit val userEmail: String = request.session.get("email").getOrElse("")
    this.disabledAfterYEReports = disabledAfterYEReports
    request.body
      .file("file")
      .map(file => {
        FileType.withNameOpt(file.contentType.getOrElse("")) match {
          case Some(contentType) =>
            var fileContentType = contentType

            // This below handle some cases where uploading csv from window gives excel contentType due to the Window registry settings.
            if (
              (fileContentType == FileType.XLS || fileContentType == FileType.XLSX) && FilenameUtils
                .getExtension(file.filename)
                .toLowerCase == "csv"
            ) {
              fileContentType = FileType.TextCSV
            }

            processFile(file.ref.toFile, file.filename, fileContentType, disabledAfterYEReports)  //No 3.

          case None =>
            encapErrorResponse(
              UnprocessableEntity,
              ExtractionErrorCode.UNSUPPORTED_REPORT_TYPE_FILE_EXTENSION
            )
        }
      })
      .getOrElse(
        encapErrorResponse(
          UnprocessableEntity,
          ExtractionErrorCode.UNRECOGNISED_ERROR
        )
      )
  }



  def processFile(
                   file: File,
                   fileName: String,
                   fileType: FileType,
                   disabledAfterYEReports: Map[String, Boolean]
                 )(implicit
                   fundEngagement: FundEngagementData,
                   fund: Fund,
                   userEmail: String
                 ): Result = encapErrorResponse(
    UnprocessableEntity,
    ExtractionErrorCode.UNRECOGNISED_ERROR
  )



  def processExcelFile(
                        filePath: String,
                        contentType: String,
                        disabledAfterYEReports: Map[String, Boolean]
                      )(implicit
                        fundEngagement: FundEngagementData,
                        fund: Fund,
                        userEmail: String
                      ): Result = {
    val workbook = GeneralExcelExtractor
      .extract(filePath, contentType)
      .getOrElse(
        throw ExtractionTabError(None, ExtractionErrorCode.UNSUPPORTED_REPORT_TYPE_FILE_EXTENSION) // Why the UNSUPPORTED_REPORT_TYPE_FILE_EXTENSION throw??
      )

    val periodStart = date2UTCLocalDate(fund.auditPeriodBegin)
    val periodEnd   = date2UTCLocalDate(fund.auditPeriodEnd)

    val sheetIterator = workbook.sheetIterator()

    var updatedTab         = ListBuffer[String]()
    var successWithWarning = new ListBuffer[WarningTabStatus]()
    var errorList          = new ListBuffer[ExtractionTabError]()

    while (sheetIterator.hasNext) {
      val sheet = sheetIterator.next()
      getTabProcessorExec(sheet, disabledAfterYEReports) match {
        case Success(value) =>
          value match {
            case (tabs, warning) =>
              if (tabs.nonEmpty) updatedTab ++= tabs
              if (warning.nonEmpty) successWithWarning ++= warning
            case _ =>
          }
        case Failure(e) =>
          e match {
            case fErr: ExtractionError =>
              errorList += ExtractionTabError(None, fErr.errorCode)
            case eErr: ExtractionTabError =>
              SafeLogger.logStringError(
                Logger,
                s"Extraction failed for fund engagement ${fundEngagement.id}",
                eErr
              )
              errorList += eErr
            case eErr: Exception =>
              SafeLogger.logStringError(
                Logger,
                s"Extraction failed for fund engagement ${fundEngagement.id}",
                eErr
              )
              errorList += ExtractionTabError(None, ExtractionErrorCode.UNRECOGNISED_ERROR)
          }
      }
    }

    if (successWithWarning.nonEmpty || errorList.nonEmpty || updatedTab.nonEmpty) {
      encapExtractionResult(
        massageWarning(updatedTab, successWithWarning.toList),
        fund.name,
        periodStart,
        periodEnd,
        massageError(updatedTab, errorList.toList)
      )
    } else {
      SafeLogger.logStringError(
        Logger,
        s"Extraction failed for fund engagement ${fundEngagement.id}",
        ExtractionException("No result & error")
      )
      encapErrorResponse(
        UnprocessableEntity,
        ExtractionErrorCode.UNRECOGNISED_ERROR
      )
    }
  }

  def getExcelReportProcessorClass(
                                    sheet: Sheet,
                                    fundName: String,
                                    fundEngagementId: String
                                  ): Option[String] = None

  def ruleMatchProcess(
                        reportMap: Map[EGATab, (String, Seq[(String, Seq[MatchRule])])],
                        fundEngagementId: String,
                        matchLogic: (MatchRule, EGATab) => Boolean
                      ): Option[String] = {
    val selectedTypeId = getSelectedReportTypeId(fundEngagementId)
    reportMap.foreach { case (reportType, config) =>
      if (
        Option(config._2).isDefined && Option(config._2).nonEmpty && selectedTypeId.contains(
          config._1
        )
      )
        config._2.foreach(processor_rule => {
          if (processor_rule._2.exists(rule => matchLogic(rule, reportType)))
            return Some(processor_rule._1)
        })
    }
    None
  }
