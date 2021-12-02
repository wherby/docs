
object WorkdayGLReader {
  val headerStr =
    "\tLedger/Budget Period\tJournal\tJournal Source\tTransaction\tAccounting Date\tLedger Account\tBook Code\tTransaction Debit Amount\tTransaction Credit Amount\tTransaction Debit minus Credit\tTransaction Currency\tJournal Line Exchange Rate\tLedger/Budget Debit Amount\tLedger/Budget Credit Amount\tLedger/Budget Debit minus Credit\tLedger Currency\tMemo\tLine Memo\tWorktags"
  val lenthFilter       = BasicExcelReaderHelper.basicRowLengthFilter(20)
  val headerTitleFilter = BasicExcelReaderHelper.basicHeaderTitleFilter(2, "Journal")
  val emptyFilter       = BasicExcelReaderHelper.basicEmptyFilter(2)

  def createRow(row: Seq[ExcelField]) = {
    (createGLRowMap() ++ Map(
      BeginingBalanceDescriptiton -> getValueOfExcelField(row, 6),
      TranDate                    -> convertDateStringToYMDString(getValueOfExcelField(row, 5)),
      TranDescription             -> getValueOfExcelField(row, 19),
      Currency                    -> getValueOfExcelField(row, 11),
      LocalAmount                 -> getValueOfExcelField(row, 10),
      BookAmount                  -> getValueOfExcelField(row, 15)
    )).toMap
  }

  val setting = BasicExcelReader(
    headerStr,
    Seq(lenthFilter, headerTitleFilter, emptyFilter),
    createRow,
    None
  )
}