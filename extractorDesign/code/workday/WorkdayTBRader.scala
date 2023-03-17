
object WorkdayTBReader {
  val headerStr =
    "\tLedger Account\tTransaction Currency\tBook Code\tTransaction Balance\tLedger Balance"
  val lengthFilter      = BasicExcelReaderHelper.basicRowLengthFilter(3)
  val headerTitleFilter = BasicExcelReaderHelper.basicHeaderTitleFilter(2, "Transaction Currency")
  val emptyFilter       = BasicExcelReaderHelper.basicEmptyFilter(4)

  def createRow(row: Seq[ExcelField]) = {
    (createTBRowMap() ++ Map(
      Description_Detail    -> getValueOfExcelField(row, 2),
      ClosingBalance_Detail -> getValueOfExcelField(row, 4),
      AccountName           -> getValueOfExcelField(row, 1),
      Balance               -> getValueOfExcelField(row, 5)
    )).toMap
  }

  val setting = BasicExcelReader(
    headerStr,
    Seq(lengthFilter, headerTitleFilter, emptyFilter),
    createRow,
    None
  )
}