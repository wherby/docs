package com.pwc.ds.awm.strategy.fundadmin

import com.pwc.ds.awm.component.dora.DoraServer
import com.pwc.ds.awm.component.safelogger.{AppLogger, SafeLogger}
import com.pwc.ds.awm.const.EGATab._
import com.pwc.ds.awm.const.ExtractionErrorCode._
import com.pwc.ds.awm.const.ExtractionWarningCode.AFTER_YE_DATA
import com.pwc.ds.awm.const.FileType._
import com.pwc.ds.awm.const.{EGATab, ExtractStatus, ExtractionErrorCode, ExtractionWarningCode, FileType, UploadFileStatus}
import com.pwc.ds.awm.controllers.JsonFormat.SecurityItemsAndPricesTemp
import com.pwc.ds.awm.db.{AccountRecord, Fund, FundEngagementData, FundEngagementReportTypeSelectionData, GSPTabData, SecurityItem}
import com.pwc.ds.awm.extractor.GeneralExcelExtractor
import com.pwc.ds.awm.processor.date2UTCLocalDate
import com.pwc.ds.awm.processor.exceptions.{ExtractionError, ExtractionException, ExtractionTabError}
import com.pwc.ds.awm.processor.generateEGA.basicReport.{GeneralSingleEGAStorage, OutPutParam, Report, SingleEGAStorageTrait, WarningTabStatus}
import com.pwc.ds.awm.services.{FundEngagementReportTypeSelectionRead, FundEngagementReportTypeSelectionWrite, SecurityItemRead}
import doracore.core.msg.Job
import org.apache.commons.io.FilenameUtils
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import play.api.Logger
import play.api.libs.Files
import play.api.libs.json._
import play.api.mvc.Results.{Ok, UnprocessableEntity}
import play.api.mvc.{MultipartFormData, Request, Result, Results}
import java.io.File
import java.sql.Timestamp
import java.time.LocalDate

import javax.inject.Inject

import scala.collection.Map
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}

case class ExtractionMsg(tabName: Seq[String], code: Int)

trait FundReportStrategy {

  def processFundReport(
                         request: Request[MultipartFormData[Files.TemporaryFile]],
                         disabledAfterYEReports: Map[String, Boolean]
                       )(implicit fundEngagement: FundEngagementData, fund: Fund): Result

  def generateSingleEGA(selection: Seq[FundEngagementReportTypeSelectionData])(
    auditPeriodEnd: java.sql.Date,
    fundName: String,
    baseCurrency: String,
    templateWorkbook: XSSFWorkbook,
    singleEGAReportTabs: Seq[OutPutParam]
  )(
                         accountMapping: Seq[AccountRecord],
                         accountRecordWithRef: Seq[Map[String, String]],
                         questionnare: JsArray,
                         accountNumberFileNameMapping: Map[String, String],
                         securityItemsAndPrices: SecurityItemsAndPricesTemp,
                         getItemAndPrice: (Seq[String], String) => Future[Seq[GSPTabData]],
                         workInvDataGUESSINGSOURCE: Map[String, Map[String, Seq[Map[String, String]]]]
                       ): XSSFWorkbook

  def getOutputParams(selection: Seq[FundEngagementReportTypeSelectionData]): Seq[OutPutParam]

  def getSingleEGAStorage(selection: Seq[FundEngagementReportTypeSelectionData]): SingleEGAStorageTrait
}

class FundReportStrategyBase @Inject() (
                                         doraServer: DoraServer,
                                         securityItemRead: SecurityItemRead,
                                         fundEngagementReportTypeSelectionRead: FundEngagementReportTypeSelectionRead,
                                         fundEngagementReportTypeSelectionWrite: FundEngagementReportTypeSelectionWrite
                                       ) extends FundReportStrategy
  with AppLogger {

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

            processFile(file.ref.toFile, file.filename, fileContentType, disabledAfterYEReports)

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

  override def generateSingleEGA(selection: Seq[FundEngagementReportTypeSelectionData])(
    auditPeriodEnd: java.sql.Date,
    fundName: String,
    baseCurrency: String,
    templateWorkbook: XSSFWorkbook,
    singleEGAReportTabs: Seq[OutPutParam]
  )(
                                  accountMapping: Seq[AccountRecord],
                                  accountRecordWithRef: Seq[Map[String, String]],
                                  questionnare: JsArray,
                                  accountNumberFileNameMapping: Map[String, String],
                                  securityItemsAndPrices: SecurityItemsAndPricesTemp,
                                  getItemAndPrice: (Seq[String], String) => Future[Seq[GSPTabData]],
                                  workInvDataGUESSINGSOURCE: Map[String, Map[String, Seq[Map[String, String]]]]
                                ): XSSFWorkbook = {
    throw ExtractionException("Invalid fund admin")
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

  def isFundNameMatch(
                       fundName: String,
                       sheet: Sheet,
                       fundCell: (Int, Int),
                       reportName: EGATab
                     ): Boolean = {
    if (
      fundName.toLowerCase().replaceAll("\\s", "") ==
        GeneralExcelExtractor
          .getContentByCell(sheet, fundCell)
          .split('\n')
          .headOption
          .getOrElse("")
          .toLowerCase()
          .trim()
          .replaceAll("\\s", "")
    )
      true
    else throw ExtractionTabError(Option(reportName), ExtractionErrorCode.FUND_NAME_MISMATCH)
  }

  def isReportNameMatch(reportName: String, sheet: Sheet, reportCell: (Int, Int)): Boolean = {
    GeneralExcelExtractor.getContentByCell(sheet, reportCell).toLowerCase() == reportName
      .toLowerCase()
  }

  def isHeaderRowMatch(
                        sheet: Sheet,
                        headerList: Seq[String],
                        headerRowRange: (Int, Int)
                      ): Boolean = {
    val rows = GeneralExcelExtractor.getContentByRow(sheet, (headerRowRange._1, headerRowRange._2))

    rows.exists(row => row.count(cell => headerList.contains(cell.trim)) == headerList.length)
  }

  def regexMatch(pattern: Regex, target: String): Boolean = {
    pattern.findFirstMatchIn(target) match {
      case Some(_) => true
      case None    => false
    }
  }

  def patchISIN(
                 addData: Seq[Map[String, String]],
                 identifyColumn: String,
                 updateColumn: String = "ISIN"
               ): Seq[Map[String, String]] = {
    val custodianList = addData.map(_.getOrElse(identifyColumn, "")).distinct
    val securityItems = Await.result(securityItemRead.getSecuritySeq(custodianList), 10 seconds)
    addData.map(data => {
      var patchedData = data
      val securityItem = securityItems
        .find(item => item.cname.toLowerCase == data(identifyColumn).toLowerCase)
        .getOrElse(
          SecurityItem("", "", "", "", "", None)
        )

      if (securityItem.isin.nonEmpty) patchedData = data + (updateColumn -> securityItem.isin)
      patchedData
    })
  }

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
        throw ExtractionTabError(None, ExtractionErrorCode.UNSUPPORTED_REPORT_TYPE_FILE_EXTENSION)
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
                logger,
                s"Extraction failed for fund engagement ${fundEngagement.id}",
                eErr
              )
              errorList += eErr
            case eErr: Exception =>
              SafeLogger.logStringError(
                logger,
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
        logger,
        s"Extraction failed for fund engagement ${fundEngagement.id}",
        ExtractionException("No result & error")
      )
      encapErrorResponse(
        UnprocessableEntity,
        ExtractionErrorCode.UNRECOGNISED_ERROR
      )
    }
  }

  def getTabProcessorExec(sheet: Sheet, disabledAfterYEReports: Map[String, Boolean])(implicit
                                                                                      fundEngagement: FundEngagementData,
                                                                                      fund: Fund,
                                                                                      userEmail: String
  ): Try[(Seq[String], Seq[WarningTabStatus])] = Try {
    val processorClass =
      getExcelReportProcessorClass(sheet, fund.name, fundEngagement.id).getOrElse(
        throw ExtractionTabError(None, ExtractionErrorCode.UNSUPPORTED_REPORT_TYPE_FILE_EXTENSION)
      )

    val data = GeneralExcelExtractor.getSheetData(sheet)

    val periodStart         = date2UTCLocalDate(fund.auditPeriodBegin)
    val periodEnd           = date2UTCLocalDate(fund.auditPeriodEnd)
    val lineBreaker: String = ""

    val paras  = List(data, fund.name, lineBreaker, periodStart, periodEnd)
    val result = doraServer.runProcess(paras, processorClass, "exec")

    execProcessor(result, fundEngagement.id, fund.name, userEmail, periodStart, periodEnd)

  }

  def encapErrorResponse(R: Results.Status, errorCode: ExtractionErrorCode): Result = {
    R(
      Json.obj("errorCodes" -> Seq(errorCode.id).distinct)
    )
  }

  def encapErrorResponse(R: Results.Status, errorCode: Seq[ExtractionErrorCode]): Result = {
    R(
      Json.obj("errorCodes" -> errorCode.map(o => o.id).distinct)
    )
  }

  def encapExtractionResult(
                             warningMsg: List[ExtractionMsg],
                             fundName: String,
                             periodStart: LocalDate,
                             periodEnd: LocalDate,
                             errorList: List[ExtractionMsg] = List[ExtractionMsg]()
                           ): Result = {

    implicit val extractionTabErrorWrites: Writes[ExtractionMsg] = (obj: ExtractionMsg) =>
      Json.obj(
        "tabName" -> obj.tabName,
        "code"    -> obj.code
      )

    Ok(
      Json.obj(
        "warning"         -> Json.toJson(warningMsg),
        "error"           -> Json.toJson(errorList),
        "fundName"        -> fundName,
        "fundStartPeriod" -> periodStart,
        "fundEndPeriod"   -> periodEnd
      )
    )
  }

  def execProcessor(
                     result: Future[Job.JobResult],
                     fundEngagementId: String,
                     fundName: String,
                     userEmail: String,
                     periodStart: LocalDate,
                     periodEnd: LocalDate
                   ): (Seq[String], Seq[WarningTabStatus]) = (Seq[String](), Seq[WarningTabStatus]())

  def exportExcelToDatabase(
                             selectionDataMap: Map[EGATab, Report],
                             fundEngagementId: String,
                             userEmail: String
                           ): Future[Seq[String]] = Future {
    var egaTabList = Seq[String]()
    val selection: Seq[FundEngagementReportTypeSelectionData] = Await.result(
      fundEngagementReportTypeSelectionRead.lookupByFundEngagementId(fundEngagementId),
      10 seconds
    )

    val newRows: Seq[Option[FundEngagementReportTypeSelectionData]] = selection.map(row => {
      val temp = selectionDataMap.find(_._2.mapId == row.sheettypesReporttypesMapId)
      temp match {
        case Some((ega, egaConfig)) =>
          val extractedData = egaConfig.getData()
          if (extractedData.nonEmpty) {
            val newRow = row.copy(
              uploadFileContent = None,
              uploadFileStatus = UploadFileStatus.success.toString,
              extractionFileContent = Some(Json.toJson(extractedData).toString),
              extractionStatus = ExtractStatus.success.toString,
              modifyby = Some(userEmail),
              modifydatetime = Some(new Timestamp(System.currentTimeMillis()))
            )
            egaTabList = egaTabList :+ ega.toString
            Some(newRow)
          } else {
            None
          }
        case None => None
      }
    })
    Await.result(
      fundEngagementReportTypeSelectionWrite.updateSelectionBatch(
        newRows.filter(row => row.isDefined).map(row => row.get)
      ),
      10 seconds
    )
    fundEngagementReportTypeSelectionWrite.writeReadyTime(fundEngagementId)
    egaTabList.filter(_.nonEmpty).distinct
  }

  def getExcelReportProcessorClass(
                                    sheet: Sheet,
                                    fundName: String,
                                    fundEngagementId: String
                                  ): Option[String] = None

  override def getOutputParams(
                                selection: Seq[FundEngagementReportTypeSelectionData]
                              ): Seq[OutPutParam] = null

  override def getSingleEGAStorage(selection: Seq[FundEngagementReportTypeSelectionData]): SingleEGAStorageTrait = null

  def massageWarning(
                      updatedTab: Seq[String],
                      warningList: List[WarningTabStatus]
                    ): List[ExtractionMsg] = {
    val extractionMsgList = groupWarning(warningList)

    if (updatedTab.distinct.length > 1)
      extractionMsgList :+ ExtractionMsg(
        updatedTab.distinct,
        ExtractionWarningCode.MULTIPLE_TAB_EXTRACTED.id
      )
    else
      extractionMsgList
  }

  def massageError(
                    updatedTab: Seq[String],
                    errorList: List[ExtractionTabError]
                  ): List[ExtractionMsg] = {
    val exceptUnsupportedReportTypeErrorList = errorList
      .filterNot(_.errorCode == ExtractionErrorCode.UNSUPPORTED_REPORT_TYPE_FILE_EXTENSION)
      .distinct
    var resultErrorList = List[ExtractionTabError]()
    //changing to ignore errors if the updatedTab is not empty
    //refer to awm-1070
    if (exceptUnsupportedReportTypeErrorList.nonEmpty && updatedTab.isEmpty)
      resultErrorList = exceptUnsupportedReportTypeErrorList
    else if (updatedTab.isEmpty)
      resultErrorList = errorList
        .filter(_.errorCode == ExtractionErrorCode.UNSUPPORTED_REPORT_TYPE_FILE_EXTENSION)
        .distinct

    groupError(resultErrorList)
  }

  def groupWarning(warningList: List[WarningTabStatus]): List[ExtractionMsg] = {
    val warningCodeList = warningList.map(_.warningCode).distinct
    warningCodeList.map(warningCode => {
      ExtractionMsg(
        warningList.filter(_.warningCode == warningCode).map(_.tabName.getOrElse("").toString),
        warningCode.id
      )
    })
  }

  def groupError(errorList: List[ExtractionTabError]): List[ExtractionMsg] = {
    val errorCodeList = errorList.map(_.errorCode).distinct
    errorCodeList.map(errCode => {
      ExtractionMsg(
        errorList.filter(_.errorCode == errCode).map(_.tabName.getOrElse("").toString),
        errCode.id
      )
    })
  }

  def getSelectedReportTypeId(fundEngagementId: String): Seq[String] = {
    Await
      .result(
        fundEngagementReportTypeSelectionRead
          .lookupByFundEngagementId(fundEngagementId),
        10 seconds
      )
      .filter(res => res.selected)
      .map(_.sheettypesReporttypesMapId)
  }

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

  //creates side effect to delete storage data of AfterYE processors when they should be ignored.
  //also removes the related warnings
  def filterAfterYearData(
                           storageData: GeneralSingleEGAStorage,
                           willFilterAfterYE: Map[String, Boolean]
                         ) = {

    willFilterAfterYE.foreach({
      case (fundReport, willFilter) => {
        willFilter match {
          case true => {
            EGATab.withNameOpt(fundReport).getOrElse("") match {
              case EGATab.PSR => storageData.purchaseAndSaleTransactionReportPSRAfterYEData = Seq()
              case EGATab.GL  => storageData.detailedGeneralLedgerReportGLAfterYEData = Seq()
              case EGATab.DIVIDEND =>
                storageData.dividendsIncomeExpenseReportDividendAfterYEData = Seq()
              case EGATab.RGL => storageData.realisedGainLossReportRGLAfterYEData = Seq()
              case _          =>
            }
            storageData.warningObject = storageData.warningObject.filter(warning => {
              //filter if the warning is equal to an ignored After year fund report and the warning code is AFTER_YE_DATA
              !((warning.tabName.get.toString.toUpperCase
                .contains(fundReport)) && (warning.warningCode == AFTER_YE_DATA))
            })
          }
          case _ =>
        }
      }
    })
  }

}
