
object EGAConstBase {
  def replaceColumn(orgSeq:Seq[ColumnWithType], columnName:String, newFormat:ColumnWithType)={
    orgSeq.map{
      col =>col.columnName match {
        case x if x == columnName => newFormat
        case _=>col
      }
    }
  }
  val positionColumnList = Seq(
    ColumnWithType("Group1"), ColumnWithType("Group2"), ColumnWithType("InvestID"), ColumnWithType("Portfolio"),
    ColumnWithType("Description"),ColumnWithType("Quantity",ExcelColumnFormat.format_double),
    ColumnWithType("MarketPrice",ExcelColumnFormat.format_double), ColumnWithType("CostBook",ExcelColumnFormat.format_double),
    ColumnWithType("MarketValueLocal",ExcelColumnFormat.format_double), ColumnWithType("MarketValueBook",ExcelColumnFormat.format_double),
    ColumnWithType("UnrealizedGainOrLoss",ExcelColumnFormat.format_double), ColumnWithType("PercentAssets",ExcelColumnFormat.format_double),
    ColumnWithType("ExtendedDescription"),ColumnWithType("Description3"), ColumnWithType("Sedol"), ColumnWithType("ISIN"),
    ColumnWithType("Ticker"), ColumnWithType("FXRate",ExcelColumnFormat.format_formula),ColumnWithType("LongShort"),
    ColumnWithType("Currency"),ColumnWithType("Custodian"), ColumnWithType("SecurityExchangeListing"),ColumnWithType("FXRate2",ExcelColumnFormat.format_double),
    ColumnWithType(PositionRow.INDUSTRY)
  )

  val cashColumnList = Seq(ColumnWithType("LongShortDescription"),ColumnWithType("GroupByInfo"),ColumnWithType("InvestDescription"),
    ColumnWithType("InvestID"),ColumnWithType("Quantity",ExcelColumnFormat.format_double),ColumnWithType("FXRate",ExcelColumnFormat.format_double),
    ColumnWithType("CostBook",ExcelColumnFormat.format_double), ColumnWithType("MarketValueBook",ExcelColumnFormat.format_double),
    ColumnWithType("BookUnrealizedGrainLoss",ExcelColumnFormat.format_double),ColumnWithType("PercentInvest",ExcelColumnFormat.format_double),
    ColumnWithType("PercentSign",ExcelColumnFormat.format_double),ColumnWithType("TBName",ExcelColumnFormat.format_string),ColumnWithType("PWCMapping",ExcelColumnFormat.format_formula))

  val PSRColumnList = Seq(ColumnWithType("TradeDate",ExcelColumnFormat.format_date),
    ColumnWithType("SettleDate",ExcelColumnFormat.format_date),ColumnWithType("TranType"), ColumnWithType("InvestID"),
    ColumnWithType("Investment"),ColumnWithType("CustodianAccount"),ColumnWithType("Quantity",ExcelColumnFormat.format_double),
    ColumnWithType("Price",ExcelColumnFormat.format_double),ColumnWithType("SEC"),ColumnWithType("LocalAmount",ExcelColumnFormat.format_double),
    ColumnWithType("BookAmount",ExcelColumnFormat.format_double),ColumnWithType("ContractDate",ExcelColumnFormat.format_date),
    ColumnWithType("TranID",ExcelColumnFormat.format_double_without_comma),ColumnWithType("GenericInvestment"),ColumnWithType("Broker"),ColumnWithType("Trader"),
    ColumnWithType("Commission",ExcelColumnFormat.format_double),ColumnWithType("Expenses",ExcelColumnFormat.format_double),
    ColumnWithType("LocalCurrency"),ColumnWithType("TotalBookAmount",ExcelColumnFormat.format_formula), ColumnWithType("Portfolio"),
    //ColumnWithType("FundCurrenty"),
    ColumnWithType("LongShort",ExcelColumnFormat.format_formula),ColumnWithType("Sedol"),ColumnWithType("ISIN"),ColumnWithType("Ticker"))

  val TBColumnList = Seq(ColumnWithType("placeholder"),ColumnWithType("Group1"),ColumnWithType("Group2"),
    ColumnWithType("FinancialAccount_Detail"),ColumnWithType("Description_Detail"),ColumnWithType("OpeningBalance_Detail",ExcelColumnFormat.format_double),
    ColumnWithType("Debits_Detail",ExcelColumnFormat.format_double), ColumnWithType("Credits_Detail",ExcelColumnFormat.format_double),
    ColumnWithType("ClosingBalance_Detail",ExcelColumnFormat.format_double),ColumnWithType("AccountName"),ColumnWithType("Balance",ExcelColumnFormat.format_formula))

  val GLColumnList = Seq(ColumnWithType("BeginingBalanceDescriptiton"),ColumnWithType("BeginingBalanceAmount",ExcelColumnFormat.format_double),
    ColumnWithType("TranDate",ExcelColumnFormat.format_date), ColumnWithType("TranID",ExcelColumnFormat.format_integer),ColumnWithType("TranDescription"),ColumnWithType("Investment"),
    ColumnWithType("Loc"),ColumnWithType("Currency"),ColumnWithType("LocalAmount",ExcelColumnFormat.format_double),
    ColumnWithType("BookAmount",ExcelColumnFormat.format_double),ColumnWithType("Balance",ExcelColumnFormat.format_double),
    ColumnWithType("EndingBalanceDescription"),ColumnWithType("EndingBalanceAmount",ExcelColumnFormat.format_double))

  val dividendColumnList = Seq(ColumnWithType("Sort",ExcelColumnFormat.format_formula),ColumnWithType("Currency"),ColumnWithType("CustAccount"),ColumnWithType("Investment_1"),ColumnWithType("InvID"),ColumnWithType("TransID", ExcelColumnFormat.format_fakeInteger),ColumnWithType("ExDate",ExcelColumnFormat.format_date),ColumnWithType("ExDateQuantity",ExcelColumnFormat.format_double),
    ColumnWithType("LocalDividendPerShareAmount", ExcelColumnFormat.format_double),ColumnWithType("WHTaxRate",ExcelColumnFormat.format_double),ColumnWithType("LocalGrossDividendIncExp",ExcelColumnFormat.format_double),ColumnWithType("LocalNetDividendIncExp",ExcelColumnFormat.format_double),ColumnWithType("LocalWHTax",ExcelColumnFormat.format_double),ColumnWithType("BookGrossDividendIncExp",ExcelColumnFormat.format_double),
    ColumnWithType("BookNetDividendIncExp",ExcelColumnFormat.format_double),ColumnWithType("BookWHTax",ExcelColumnFormat.format_double),ColumnWithType("PayDate",ExcelColumnFormat.format_date),ColumnWithType("LocalReclaim", ExcelColumnFormat.format_double),ColumnWithType("BookReclaim", ExcelColumnFormat.format_double),ColumnWithType("Sedol"),ColumnWithType("ISIN_1"),ColumnWithType("Ticker_1"),ColumnWithType("Investment_1")
  )

  val RGLColumnList = Seq(ColumnWithType("Group1" ),ColumnWithType("InvestmentCode"),
    ColumnWithType("CloseDate",ExcelColumnFormat.format_date),ColumnWithType("ClosingID", ExcelColumnFormat.format_double_without_comma),ColumnWithType("TransactionType"),ColumnWithType("TaxLotDate",ExcelColumnFormat.format_date),
    ColumnWithType("TaxLotID", ExcelColumnFormat.format_double_without_comma),ColumnWithType("TaxLotPrice",ExcelColumnFormat.format_double), ColumnWithType("closingPrice", ExcelColumnFormat.format_double),ColumnWithType("OriginalFace"),
    ColumnWithType("QuantityOrCurrentFace",ExcelColumnFormat.format_double),ColumnWithType("NetProceedsBook",ExcelColumnFormat.format_double),ColumnWithType("CostBook",ExcelColumnFormat.format_double),
    ColumnWithType("PriceGLBook",ExcelColumnFormat.format_double),ColumnWithType("FXGainLoss",ExcelColumnFormat.format_double),ColumnWithType("STCapitalGL",ExcelColumnFormat.format_double), ColumnWithType("LTCapitalGL",ExcelColumnFormat.format_double),
    ColumnWithType("OrdinaryIncome",ExcelColumnFormat.format_double),ColumnWithType("TotalGLBook",ExcelColumnFormat.format_double),ColumnWithType("NetProceedsLocal",ExcelColumnFormat.format_double),ColumnWithType("CostLocal",ExcelColumnFormat.format_double),
    ColumnWithType("PriceGLLocal",ExcelColumnFormat.format_double),ColumnWithType("Sedol"),ColumnWithType("ISIN"),ColumnWithType("Ticker"),ColumnWithType("Currency"),ColumnWithType("TradeDate"),ColumnWithType("Section988IncomeExpenseCash"),ColumnWithType("Section988IncomeExpenseInvestments"))
}
