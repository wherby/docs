package com.pwc.ds.awm.processor.morgan.excelreader

import com.pwc.ds.awm.component.excel.{ExcelField, ExcelToArray}
import com.pwc.ds.awm.const.ExtractionErrorCode
import com.pwc.ds.awm.processor.SSCexcelReport.excelreader.ExcelReaderHelper
import com.pwc.ds.awm.processor.SSCexcelReport.excelreader.ExcelReaderHelper.getValueOfExcelField
import com.pwc.ds.awm.processor.exceptions.{AWMNameException, ExtractionError}
import com.pwc.ds.awm.processor.fargo.excelreader.FargoExcelCashReader.{getFXRate, getLongShortDescription}
import com.pwc.ds.awm.processor.singleegarow.PSRRow.{BookAmount, Broker, Commission, ContractDate, CustodianAccount, Expenses, FundCurrenty, GenericInvestment, ISIN, InvestID, Investment, LocalAmount, LocalCurrency, LongShort, Portfolio, Price, Quantity, SEC, Sedol, SettleDate, Ticker, TotalBookAmount, TradeDate, Trader, TranID, TranType}
import com.pwc.ds.awm.processor.{NA_STR, convertDateStringToYMDString}

object MorganExcelPSRReader {
  val psrHeaderOrg = "Portfolio ID\tPortfolio Name\tCategory Desc1\tCategory Desc2\tCUSIP\tSecurity Description\tTransaction Category\tBuy Sell Ind\tOpen/Close Ind\tTrade Date\tSettlement Date\tDate Acquired\tIssue Currency\tQuantity\tCost Change (Issue)\tCost Change (Base)\tLedger Journal Amount(Issue)\tLedger Journal Amount(Base)\tNet Amount (Settlement)\tTransaction Journal Desc\tOrder No\tGroup ID\tLedger Account Number\tLedger Account Name\tProcess Date\tCustodian\tMoney Manager\tDeal ID\tContract ID\tPosition Type\tAsset Type\tAsset Class Level1\tAsset Class Level2\tDebit/Credit\tCancel Ind\tHas Been Cancelled\tisManual"
  val psrHeader = ExcelReaderHelper.stringNormalizer(psrHeaderOrg)

  def readPSRSheet(filePath:String, fundName:String):Seq[Map[String,String]]={
    val psrSheet = ExcelToArray.multiSheetToArray(filePath)
    val psrSheetVerify = ExcelReaderHelper.getSpecifiedSheet(psrSheet,psrHeader)
    val res= psrSheetVerify.filter(row =>row.length >2 && row(1).field.toLowerCase.trim == fundName.toLowerCase.trim).map{
      row => createPSRRow(row)
    }
    if(psrSheetVerify.length >1 && res.length ==0){
      throw ExtractionError(ExtractionErrorCode.FUND_NAME_MISMATCH)
    }
    res
  }

  def createPSRRow(row:Seq[ExcelField])={
    Map(
      TradeDate -> convertDateStringToYMDString(getValueOfExcelField(row,9)),
      SettleDate -> convertDateStringToYMDString(getValueOfExcelField(row,10)),
      TranType -> getValueOfExcelField(row,7),
      InvestID -> getValueOfExcelField(row,4),
      Investment -> getValueOfExcelField(row,5),
      CustodianAccount -> getValueOfExcelField(row,25),
      Quantity -> getValueOfExcelField(row,13),
      Price -> NA_STR,
      SEC -> NA_STR,
      LocalAmount -> getValueOfExcelField(row,14),
      BookAmount -> getValueOfExcelField(row,15),
      ContractDate -> NA_STR,
      TranID -> getValueOfExcelField(row,28),
      GenericInvestment ->  getValueOfExcelField(row,32),
      Broker ->NA_STR,
      Trader -> NA_STR,
      Commission ->NA_STR ,
      Expenses -> NA_STR,
      LocalCurrency -> getValueOfExcelField(row,12),
      TotalBookAmount -> NA_STR,
      Portfolio -> getValueOfExcelField(row,1),
      FundCurrenty -> NA_STR,
      LongShort ->getValueOfExcelField(row,23),
      Sedol -> NA_STR,
      ISIN -> NA_STR,
      Ticker -> NA_STR
    )
  }
}
