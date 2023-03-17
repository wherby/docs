import java.text.SimpleDateFormat

import scala.collection.Map

class GeneralSingleEGAStorage {
  this: EGAColConstBase =>
  var errorMsg: ErrorStatus = ErrorStatus(false,"")
  var warningMsg: WarningStatus = WarningStatus(false,"")

  var trialBalanceReportTBData: Seq[Map[String, String]] = Seq()

  var detailedGeneralLedgerReportGLData: Seq[Map[String, String]] = Seq()
  var detailedGeneralLedgerReportGLAfterYEData: Seq[Map[String, String]] = Seq()

  var positionAppraisalReportPositionData: Seq[Map[String, String]] = Seq()

  var cashAppraisalReportCashData: Seq[Map[String, String]] = Seq()

  var purchaseAndSaleTransactionReportPSRData: Seq[Map[String, String]] = Seq()
  var purchaseAndSaleTransactionReportPSRAfterYEData: Seq[Map[String, String]] = Seq()

  var dividendsIncomeExpenseReportDividendData: Seq[Map[String, String]] = Seq()
  var dividendsIncomeExpenseReportDividendAfterYEData: Seq[Map[String, String]] = Seq()

  var realisedGainLossReportRGLData: Seq[Map[String, String]] = Seq()
  var realisedGainLossReportRGLAfterYEData: Seq[Map[String, String]] = Seq()

  lazy val positionParam = OutPutParam("Position",positionColumnList, () => {this.positionAppraisalReportPositionData},1)
  lazy val cashParam = OutPutParam("Cash",cashColumnList,() => {this.cashAppraisalReportCashData},1)
  lazy val PSRParam = OutPutParam("PSR",PSRColumnList,() => {this.purchaseAndSaleTransactionReportPSRData} ,1)
  lazy val PSRAfterYEParam = OutPutParam("PSR (after YE)",PSRColumnList,() => {this.purchaseAndSaleTransactionReportPSRAfterYEData},1)
  lazy val TBParam = OutPutParam("TB",TBColumnList, () => {this.trialBalanceReportTBData},3)
  lazy val GLParam = OutPutParam("GL",GLColumnList,() => {this.detailedGeneralLedgerReportGLData},1)
  lazy val GLAfterYEParam = OutPutParam("GL (after YE)",GLColumnList,() => {this.detailedGeneralLedgerReportGLAfterYEData},1)
  lazy val dividendParam = OutPutParam("Dividend",dividendColumnList,() => {this.dividendsIncomeExpenseReportDividendData},1)
  lazy val dividendAfterYEParam = OutPutParam("Dividend (after YE)",dividendColumnList,() => {this.dividendsIncomeExpenseReportDividendAfterYEData},1)
  lazy val RGLParam = OutPutParam("RGL",RGLColumnList,() => {this.realisedGainLossReportRGLData},1)
  lazy val RGLAfterYEParam = OutPutParam("RGL (after YE)",RGLColumnList,() => {this.realisedGainLossReportRGLAfterYEData},1)

  lazy val outputParams = Seq(positionParam,cashParam,PSRParam,PSRAfterYEParam,TBParam,GLParam,GLAfterYEParam,dividendParam,dividendAfterYEParam,RGLParam,RGLAfterYEParam)

}