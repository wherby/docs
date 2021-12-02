
object WorkdayGLReader2 {
  val headerStr =
    "Ledger Account\tJournal Entry\tCompany\tJournal Source\tTransaction\tAccounting Date\tMemo\tDebit\tCredit\tBalance\tNet Amount"
  val lengthFilter      = BasicExcelReaderHelper.basicRowLengthFilter(4)
  val headerTitleFilter = BasicExcelReaderHelper.basicHeaderTitleFilter(1, "Journal Entry")
  val emptyFilter       = BasicExcelReaderHelper.basicEmptyFilter(9)

  def getCurrency(inputFile: String): String = {
    val seqListValue = BasicExcelReaderHelper.defaultExcelReaderForExcel(headerStr)(inputFile)
    getValueOfExcelField(seqListValue(6), 1) //Hardcode for header value of "Translation Currency"
  }

  def getGroupedValue(inputFile: String): Map[String, Seq[Seq[ExcelField]]] = {
    val seqListValue = BasicExcelReaderHelper.defaultExcelReaderForExcel(headerStr)(inputFile)
    seqListValue.groupBy(list => list(0).field)
  }

  def getAmount(groupedRecord: Map[String, Seq[Seq[ExcelField]]], account: String): String = {
    val lastRow = groupedRecord.filter(tempGroup => tempGroup._1 == account).head._2.last
    getValueOfExcelField(lastRow, 9)
  }

  def createRow(currency: String, groupedRecord: Map[String, Seq[Seq[ExcelField]]]) =
    (row: Seq[ExcelField]) => {
      (createGLRowMap() ++ Map(
        BeginingBalanceDescriptiton -> getValueOfExcelField(row, 0),
        TranDate                    -> convertDateStringToYMDString(getValueOfExcelField(row, 5)),
        TranID                      -> getValueOfExcelField(row, 1),
        TranDescription             -> getValueOfExcelField(row, 6),
        Currency                    -> currency,
        LocalAmount                 -> getValueOfExcelFieldWithDefault(row, 10, Some("")),
        BookAmount                  -> getValueOfExcelFieldWithDefault(row, 10, Some("")),
        Balance                     -> getValueOfExcelField(row, 9),
        EndingBalanceAmount         -> getAmount(groupedRecord, getValueOfExcelField(row, 0))
      )).toMap
    }

  def setting = (currency: String, groupedRecord: Map[String, Seq[Seq[ExcelField]]]) => {
    BasicExcelReader(
      headerStr,
      Seq(lengthFilter, headerTitleFilter, emptyFilter),
      createRow(currency, groupedRecord),
      None
    )
  }
}