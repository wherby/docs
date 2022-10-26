package com.pwc.ds.awm.strategy.fundadmin

import com.pwc.ds.awm.component.dora.DoraServer
import com.pwc.ds.awm.const.EGATab.{CASH, DIVIDEND, DIVIDEND_AFTER_YE, EGATab, POSITION, PSR, PSR_AFTER_YE, RGL, RGL_AFTER_YE, TB}
import com.pwc.ds.awm.component.safelogger.SafeLogger
import com.pwc.ds.awm.const.{ConstVar, ExtractionErrorCode, FileType}
import com.pwc.ds.awm.controllers.JsonFormat.SecurityItemsAndPricesTemp
import com.pwc.ds.awm.db.{AccountRecord, Fund, FundEngagementData, FundEngagementReportTypeSelectionData, GSPTabData}
import com.pwc.ds.awm.extractor.CsvExtractor
import com.pwc.ds.awm.processor.date2UTCLocalDate
import com.pwc.ds.awm.processor.exceptions.{ExtractionError, ExtractionTabError}
import com.pwc.ds.awm.processor.generateEGA.basicReport.{MaplesEGAStorage, OutPutParam, SingleEGAExporterXLS, SingleEGAStorageTrait, WarningTabStatus}
import com.pwc.ds.awm.processor.generateEGA.confirmationValuation.Confirmation
import com.pwc.ds.awm.processor.maples._
import com.pwc.ds.awm.services.{FundEngagementReportTypeSelectionRead, FundEngagementReportTypeSelectionWrite, MultifondsUploadHelper, SecurityItemRead}
import doracore.util.ProcessService.ProcessResult
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import play.api.Logger
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.Results.UnprocessableEntity
import play.api.mvc._
import java.io.File
import java.time.LocalDate

import com.pwc.ds.awm.const.FileType._
import doracore.core.msg.Job
import javax.inject.Inject

import scala.collection.Map
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.runtime.universe.typeOf
import scala.util.{Failure, Success, Try}

class MaplesFundReportStrategy @Inject() (
                                           doraServer: DoraServer,
                                           multifondsUploadHelper: MultifondsUploadHelper,
                                           fundEngagementReportTypeSelectionRead: FundEngagementReportTypeSelectionRead,
                                           fundEngagementReportTypeSelectionWrite: FundEngagementReportTypeSelectionWrite,
                                           securityItemRead: SecurityItemRead
                                         ) extends FundReportStrategyBase(
  doraServer: DoraServer,
  securityItemRead: SecurityItemRead,
  fundEngagementReportTypeSelectionRead: FundEngagementReportTypeSelectionRead,
  fundEngagementReportTypeSelectionWrite: FundEngagementReportTypeSelectionWrite
) {

  val map: Map[EGATab, (String, Seq[(String, Seq[MatchRule])])] = Map(
    TB -> ("20001",
      Seq(
        (
          typeOf[TrialBalanceExcelReportProcessor].toString,
          Seq(
            ExcelMatchRule(
              "",
              (-1, -1),
              None,
              (-1, -1),
              Some(
                Seq(
                  "Financial Account",
                  "Description",
                  "Opening Balance",
                  "Debits",
                  "Credits",
                  "Closing Balance"
                )
              ),
              (0, 19)
            )
          )
        )
      )),
    POSITION -> ("20002",
      Seq(
        (
          typeOf[PositionAppraisalReportProcessor].toString,
          Seq(
            ExcelMatchRule("INVESTMENT POSITION APPRAISAL", (13, 0), None, (1, 3)),
            CSVMatchRule(
              Seq(
                "ReportMode",
                "LongShortDescription",
                "SortKey",
                "LocalCurrency",
                "BasketInvestDescription",
                "Description",
                "InvestID",
                "Quantity",
                "LocalPrice",
                "CostLocal",
                "CostBook",
                "BookUnrealizedGainOrLoss",
                "AccruedInterest",
                "MarketValueBook",
                "Invest"
              )
            )
          )
        )
      )),
    CASH -> ("20003",
      Seq(
        (
          typeOf[CashAppraisalReportProcessor].toString,
          Seq(
            ExcelMatchRule("Cash appraisal", (12, 0), None, (1, 3)),
            CSVMatchRule(
              Seq(
                "LongShortDescription",
                "GroupByInfo",
                "InvestDescription",
                "InvestID",
                "Quantity",
                "FXRate",
                "CostBook",
                "MarketValueBook",
                "BookUnrealizedGainLoss",
                "percentInvest",
                "percentSign"
              )
            )
          )
        )
      )),
    PSR -> ("20004",
      Seq(
        (
          typeOf[PurchaseSaleReportProcessor].toString,
          Seq(
            ExcelMatchRule(
              "",
              (-1, -1),
              None,
              (-1, -1),
              Some(
                Seq(
                  "TradeDate",
                  "SettleDate",
                  "TranType",
                  "InvestID",
                  "Investment",
                  "CustodianAccount",
                  "Quantity",
                  "Price",
                  "SEC",
                  "LocalAmount",
                  "BookAmount",
                  "ContractDate",
                  "TranID",
                  "GenericInvestment",
                  "Broker",
                  "Trader",
                  "Commission",
                  "Expenses",
                  "LocalCurrency",
                  "TotalBookAmount"
                )
              ),
              (0, 0)
            ),
            ExcelMatchRule(
              "",
              (-1, -1),
              None,
              (-1, -1),
              Some(
                Seq(
                  "TradeDate",
                  "SettleDate",
                  "TranType",
                  "InvestID",
                  "Investment",
                  "CustodianAccount",
                  "Quantity",
                  "Price",
                  "SEC",
                  "LocalAmount",
                  "Absolute book amount",
                  "BookAmount",
                  "ContractDate",
                  "TranID",
                  "GenericInvestment",
                  "Broker",
                  "Trader",
                  "Commission",
                  "Expenses",
                  "LocalCurrency",
                  "TotalBookAmount"
                )
              ),
              (0, 0)
            ),
            CSVMatchRule(
              Seq(
                "TradeDate",
                "SettleDate",
                "TranType",
                "InvestID",
                "Investment",
                "CustodianAccount",
                "Quantity",
                "Price",
                "SEC",
                "LocalAmount",
                "BookAmount",
                "ContractDate",
                "TranID",
                "GenericInvestment",
                "Broker",
                "Trader",
                "Commission",
                "Expenses",
                "LocalCurrency",
                "TotalBookAmount"
              )
            )
          )
        )
      )),
    PSR_AFTER_YE -> ("20005",
      Seq(
        (
          typeOf[PurchaseSaleReportProcessor].toString,
          Seq(
            ExcelMatchRule(
              "",
              (-1, -1),
              None,
              (-1, -1),
              Some(
                Seq(
                  "TradeDate",
                  "SettleDate",
                  "TranType",
                  "InvestID",
                  "Investment",
                  "CustodianAccount",
                  "Quantity",
                  "Price",
                  "SEC",
                  "LocalAmount",
                  "BookAmount",
                  "ContractDate",
                  "TranID",
                  "GenericInvestment",
                  "Broker",
                  "Trader",
                  "Commission",
                  "Expenses",
                  "LocalCurrency",
                  "TotalBookAmount"
                )
              ),
              (0, 0)
            ),
            ExcelMatchRule(
              "",
              (-1, -1),
              None,
              (-1, -1),
              Some(
                Seq(
                  "TradeDate",
                  "SettleDate",
                  "TranType",
                  "InvestID",
                  "Investment",
                  "CustodianAccount",
                  "Quantity",
                  "Price",
                  "SEC",
                  "LocalAmount",
                  "Absolute book amount",
                  "BookAmount",
                  "ContractDate",
                  "TranID",
                  "GenericInvestment",
                  "Broker",
                  "Trader",
                  "Commission",
                  "Expenses",
                  "LocalCurrency",
                  "TotalBookAmount"
                )
              ),
              (0, 0)
            ),
            CSVMatchRule(
              Seq(
                "TradeDate",
                "SettleDate",
                "TranType",
                "InvestID",
                "Investment",
                "CustodianAccount",
                "Quantity",
                "Price",
                "SEC",
                "LocalAmount",
                "BookAmount",
                "ContractDate",
                "TranID",
                "GenericInvestment",
                "Broker",
                "Trader",
                "Commission",
                "Expenses",
                "LocalCurrency",
                "TotalBookAmount"
              )
            )
          )
        )
      )),
    DIVIDEND -> ("20006",
      Seq(
        (
          typeOf[DividendReportProcessor].toString,
          Seq(
            ExcelMatchRule(
              "",
              (-1, -1),
              None,
              (-1, -1),
              Some(
                Seq(
                  "Sort",
                  "Currency",
                  "CustAccount",
                  "Investment",
                  "InvID",
                  "TransID",
                  "ExDate",
                  "ExDateQuantity",
                  "LocalDividendPerShareAmount",
                  "WHTaxRate",
                  "LocalGrossDividendIncExp",
                  "LocalWHTax",
                  "LocalNetDividendIncExp",
                  "LocalWHTax",
                  "BookGrossDividendIncExp",
                  "BookWHTax",
                  "BookNetDividendIncExp",
                  "BookWHTax",
                  "PayDate",
                  "LocalReclaim",
                  "BookReclaim"
                )
              ),
              (0, 1)
            ),
            CSVMatchRule(
              Seq(
                "Sort",
                "Currency",
                "CustAccount",
                "Investment",
                "Investment",
                "TransID",
                "ExDate",
                "ExDateQuantity",
                "LocalDividendPerShareAmount",
                "WHTaxRate",
                "LocalGrossDividendIncExp",
                "LocalWHTax",
                "LocalNetDividendIncExp",
                "LocalWHTax",
                "BookGrossDividendIncExp",
                "BookWHTax",
                "BookNetDividendIncExp",
                "BookWHTax",
                "PayDate",
                "LocalReclaim",
                "BookReclaim",
                "LocalRelief",
                "BookRelief"
              )
            )
          )
        )
      )),
    DIVIDEND_AFTER_YE -> ("20007",
      Seq(
        (
          typeOf[DividendReportProcessor].toString,
          Seq(
            ExcelMatchRule(
              "",
              (-1, -1),
              None,
              (-1, -1),
              Some(
                Seq(
                  "Sort",
                  "Currency",
                  "CustAccount",
                  "Investment",
                  "InvID",
                  "TransID",
                  "ExDate",
                  "ExDateQuantity",
                  "LocalDividendPerShareAmount",
                  "WHTaxRate",
                  "LocalGrossDividendIncExp",
                  "LocalWHTax",
                  "LocalNetDividendIncExp",
                  "LocalWHTax",
                  "BookGrossDividendIncExp",
                  "BookWHTax",
                  "BookNetDividendIncExp",
                  "BookWHTax",
                  "PayDate",
                  "LocalReclaim",
                  "BookReclaim"
                )
              ),
              (0, 1)
            ),
            CSVMatchRule(
              Seq(
                "Sort",
                "Currency",
                "CustAccount",
                "Investment",
                "Investment",
                "TransID",
                "ExDate",
                "ExDateQuantity",
                "LocalDividendPerShareAmount",
                "WHTaxRate",
                "LocalGrossDividendIncExp",
                "LocalWHTax",
                "LocalNetDividendIncExp",
                "LocalWHTax",
                "BookGrossDividendIncExp",
                "BookWHTax",
                "BookNetDividendIncExp",
                "BookWHTax",
                "PayDate",
                "LocalReclaim",
                "BookReclaim",
                "LocalRelief",
                "BookRelief"
              )
            )
          )
        )
      )),
    RGL -> ("20008",
      Seq(
        (
          typeOf[RealisedGainLossProcessor].toString,
          Seq(
            ExcelMatchRule(
              "",
              (-1, -1),
              None,
              (-1, -1),
              Some(
                Seq(
                  "Group1",
                  "InvestmentCode",
                  "CloseDate",
                  "ClosingID",
                  "TransactionType",
                  "TaxLotDate",
                  "TaxLotID",
                  "TaxLotPrice",
                  "ClosingPrice",
                  "OriginalFace",
                  "QuantityOrCurrentFace",
                  "NetProceedsBook",
                  "CostBook",
                  "PriceGLBook",
                  "FXGainLoss",
                  "STCapitalGL",
                  "LTCapitalGL",
                  "OrdinaryIncome",
                  "TotalGLBook",
                  "NetProceedsLocal",
                  "CostLocal",
                  "PriceGLLocal"
                )
              ),
              (0, 0)
            ),
            ExcelMatchRule(
              "REALIZED GAIN LOSS REPORT",
              (10, 0),
              None,
              (1, 3)
            ),
            CSVMatchRule(
              Seq(
                "Group1",
                "InvestmentCode",
                "CloseDate",
                "ClosingID",
                "TransactionType",
                "TaxLotDate",
                "TaxLotID",
                "TaxLotPrice",
                "ClosingPrice",
                "OriginalFace",
                "QuantityOrCurrentFace",
                "NetProceedsBook",
                "CostBook",
                "PriceGLBook",
                "FXGainLoss",
                "STCapitalGL",
                "LTCapitalGL",
                "OrdinaryIncome",
                "TotalGLBook",
                "NetProceedsLocal",
                "CostLocal",
                "PriceGLLocal"
              )
            )
          )
        )
      )),
    RGL_AFTER_YE -> ("20009",
      Seq(
        (
          typeOf[RealisedGainLossProcessor].toString,
          Seq(
            ExcelMatchRule(
              "",
              (-1, -1),
              None,
              (-1, -1),
              Some(
                Seq(
                  "Group1",
                  "InvestmentCode",
                  "CloseDate",
                  "ClosingID",
                  "TransactionType",
                  "TaxLotDate",
                  "TaxLotID",
                  "TaxLotPrice",
                  "ClosingPrice",
                  "OriginalFace",
                  "QuantityOrCurrentFace",
                  "NetProceedsBook",
                  "CostBook",
                  "PriceGLBook",
                  "FXGainLoss",
                  "STCapitalGL",
                  "LTCapitalGL",
                  "OrdinaryIncome",
                  "TotalGLBook",
                  "NetProceedsLocal",
                  "CostLocal",
                  "PriceGLLocal"
                )
              ),
              (0, 0)
            ),
            ExcelMatchRule(
              "REALIZED GAIN LOSS REPORT",
              (10, 0),
              None,
              (1, 3)
            ),
            CSVMatchRule(
              Seq(
                "Group1",
                "InvestmentCode",
                "CloseDate",
                "ClosingID",
                "TransactionType",
                "TaxLotDate",
                "TaxLotID",
                "TaxLotPrice",
                "ClosingPrice",
                "OriginalFace",
                "QuantityOrCurrentFace",
                "NetProceedsBook",
                "CostBook",
                "PriceGLBook",
                "FXGainLoss",
                "STCapitalGL",
                "LTCapitalGL",
                "OrdinaryIncome",
                "TotalGLBook",
                "NetProceedsLocal",
                "CostLocal",
                "PriceGLLocal"
              )
            )
          )
        )
      ))
  )

  var storage = new MaplesEGAStorage(map)

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

    val singleMaplesStorage = getStorage(selection)
    var workbook = SingleEGAExporterXLS.exportToExcelWorkbook(
      auditPeriodEnd,
      fundName,
      baseCurrency,
      templateWorkbook,
      singleEGAReportTabs,
      singleMaplesStorage.outputDateFormat
    )
    val cashParam     = singleMaplesStorage.cashParam.sheetContent()
    val positionParam = singleMaplesStorage.positionParam.sheetContent()

    Confirmation.generateConfirmationAndValuationToWorkbook(
      selection,
      cashParam,
      positionParam,
      accountMapping,
      accountRecordWithRef,
      questionnare,
      accountNumberFileNameMapping,
      securityItemsAndPrices,
      auditPeriodEnd,
      getItemAndPrice,
      workInvDataGUESSINGSOURCE,
      workbook,
      securityItemRead
    )
  }

  override def getOutputParams(
                                selection: Seq[FundEngagementReportTypeSelectionData]
                              ): Seq[OutPutParam] = getStorage(selection).outputParams


  override def getSingleEGAStorage(selection: Seq[FundEngagementReportTypeSelectionData]): SingleEGAStorageTrait = getStorage(selection)

  //  // ---- Class Methods --------------------------------

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
      case FileType.TextCSV =>
        val periodStart = date2UTCLocalDate(fund.auditPeriodBegin)
        val periodEnd   = date2UTCLocalDate(fund.auditPeriodEnd)

        val data = CsvExtractor.extract(file.getPath)

        // given that CSV file only consist one fund report data
        getProcessorCSVExec(data) match {
          case Success(value) =>
            value match {
              case (tabs, warning: Seq[WarningTabStatus]) =>
                if (warning.nonEmpty)
                  encapExtractionResult(
                    groupWarning(warning.toList),
                    fund.name,
                    periodStart,
                    periodEnd
                  )
                else
                  encapExtractionResult(
                    List[ExtractionMsg](),
                    fund.name,
                    periodStart,
                    periodEnd
                  )
            }
          case Failure(e) =>
            e match {
              case e: ExtractionError =>
                SafeLogger.logStringError(
                  logger,
                  s"Extraction failed for fund engagement ${fundEngagement.id}",
                  e
                )
                encapErrorResponse(
                  UnprocessableEntity,
                  e.errorCode
                )
              case e: ExtractionTabError =>
                encapExtractionResult(
                  List[ExtractionMsg](),
                  fund.name,
                  periodStart,
                  periodEnd,
                  groupError(List[ExtractionTabError](e))
                )
              case e: Exception =>
                SafeLogger.logStringError(
                  logger,
                  s"Extraction failed by unknown reason for fund engagement ${fundEngagement.id}",
                  e
                )
                encapErrorResponse(
                  UnprocessableEntity,
                  ExtractionErrorCode.UNRECOGNISED_ERROR
                )
            }
        }

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

  override def execProcessor(
                              result: Future[Job.JobResult],
                              fundEngagementId: String,
                              fundName: String,
                              userEmail: String,
                              periodStart: LocalDate,
                              periodEnd: LocalDate
                            ): (Seq[String], Seq[WarningTabStatus]) = {
    val newStorage = Await.result(result, ConstVar.ProcessJobWaitTime seconds)
    //check whether the result has exception´
    multifondsUploadHelper.checkJobResultException(newStorage)
    newStorage.result match {
      case newStorageResult: ProcessResult =>
        newStorageResult.result match {
          case resultStorage: MaplesEGAStorage =>
            //filters out the afterYE reports data and warning if the user did not select this in the frontend
            filterAfterYearData(resultStorage, disabledAfterYEReports)
            //store the extracted data in database
            val updatedTab = Await.result(
              exportExcelToDatabase(resultStorage, fundEngagementId, userEmail),
              ConstVar.ProcessJobWaitTime seconds
            )
            (updatedTab, resultStorage.warningObject)

          case _ =>
            throw new Exception("Extraction failed. No EGA result")
        }
      case _ =>
        throw new Exception("Extract Not Success. No Doro job result")
    }
  }

  def getCSVReportProcessorClass(
                                  data: Seq[Seq[String]],
                                  fundName: String,
                                  fundEngagementId: String
                                ): Option[String] = {

    ruleMatchProcess(
      map,
      fundEngagementId,
      (rule: MatchRule, reportType: EGATab) =>
        rule match {
          case CSVMatchRule(header) => header == data.headOption.getOrElse(Seq(""))
          case _                    => false
        }
    )
  }

  def getProcessorCSVExec(data: Seq[Seq[String]])(implicit
                                                  fundEngagement: FundEngagementData,
                                                  fund: Fund,
                                                  userEmail: String
  ): Try[(Seq[String], Seq[WarningTabStatus])] = Try {
    val processorClass = getCSVReportProcessorClass(data, fund.name, fundEngagement.id).getOrElse(
      throw ExtractionError(ExtractionErrorCode.UNSUPPORTED_REPORT_TYPE_FILE_EXTENSION)
    )

    val periodStart         = date2UTCLocalDate(fund.auditPeriodBegin)
    val periodEnd           = date2UTCLocalDate(fund.auditPeriodEnd)
    var lineBreaker: String = ""

    val paras  = List(data, fund.name, lineBreaker, periodStart, periodEnd)
    val result = doraServer.runProcess(paras, processorClass, "execCSV")

    execProcessor(result, fundEngagement.id, fund.name, userEmail, periodStart, periodEnd)
  }

  def exportExcelToDatabase(
                             storage: MaplesEGAStorage,
                             fundEngagementId: String,
                             userEmail: String
                           ): Future[Seq[String]] = {
    storage.setReportConfig(map)
    super.exportExcelToDatabase(storage.reports, fundEngagementId, userEmail)
  }

  private def getStorage(
                          selection: Seq[FundEngagementReportTypeSelectionData]
                        ): MaplesEGAStorage = {
    var singleEGAStorage = new MaplesEGAStorage(map)
    selection
      .filter(select => select.extractionFileContent.isDefined && select.selected)
      .foreach(row => {
        val str     = row.extractionFileContent.getOrElse("[]")
        val addData = Json.parse(str).as[Seq[Predef.Map[String, String]]]
        singleEGAStorage.reports.find { case (reportType, report) =>
          report.mapId == row.sheettypesReporttypesMapId
        } match {
          case Some((reportType, report)) =>
            reportType match {
              case POSITION =>
                report.setData(
                  patchISIN(addData, "Description")
                )
              case PSR | PSR_AFTER_YE =>
                report.setData(
                  patchISIN(addData, "Investment")
                )
              case DIVIDEND | DIVIDEND_AFTER_YE =>
                report.setData(
                  patchISIN(addData, "Investment_1", "ISIN_1")
                )
              case _ =>
                report.setData(addData)
            }
          case None =>
        }
      })
    singleEGAStorage
  }
}
