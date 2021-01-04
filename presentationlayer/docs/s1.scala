import java.text.SimpleDateFormat

import scala.collection.Map

class FargoSingleEGAStorage {
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

  val positionParam = OutPutParam("Position",EGAConstFargo.positionColumnList, () => {this.positionAppraisalReportPositionData},1)
  val cashParam = OutPutParam("Cash",EGAConstFargo.cashColumnList,() => {this.cashAppraisalReportCashData},1)
  val PSRParam = OutPutParam("PSR",EGAConstFargo.PSRColumnList,() => {this.purchaseAndSaleTransactionReportPSRData} ,1)
  val PSRAfterYEParam = OutPutParam("PSR (after YE)",EGAConstFargo.PSRColumnList,() => {this.purchaseAndSaleTransactionReportPSRAfterYEData},1)
  val TBParam = OutPutParam("TB",EGAConstFargo.TBColumnList, () => {this.trialBalanceReportTBData},3)
  val GLParam = OutPutParam("GL",EGAConstFargo.GLColumnList,() => {this.detailedGeneralLedgerReportGLData},1)
  val GLAfterYEParam = OutPutParam("GL (after YE)",EGAConstFargo.GLColumnList,() => {this.detailedGeneralLedgerReportGLAfterYEData},1)
  val dividendParam = OutPutParam("Dividend",EGAConstFargo.dividendColumnList,() => {this.dividendsIncomeExpenseReportDividendData},1)
  val dividendAfterYEParam = OutPutParam("Dividend (after YE)",EGAConstFargo.dividendColumnList,() => {this.dividendsIncomeExpenseReportDividendAfterYEData},1)
  val RGLParam = OutPutParam("RGL",EGAConstFargo.RGLColumnList,() => {this.realisedGainLossReportRGLData},1)
  val RGLAfterYEParam = OutPutParam("RGL (after YE)",EGAConstFargo.RGLColumnList,() => {this.realisedGainLossReportRGLAfterYEData},1)

  val outputParams = Seq(positionParam,cashParam,PSRParam,PSRAfterYEParam,TBParam,GLParam,GLAfterYEParam,dividendParam,dividendAfterYEParam,RGLParam,RGLAfterYEParam)
  val outputDateFormat = Some(new SimpleDateFormat("yyyy/MM/dd"))
}
