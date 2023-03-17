import scala.collection.immutable.ListMap
import scala.collection.mutable.ListBuffer

def extractProfitLossSheetFromExcel(frontEndFinancialData:FinancialdataGetFromFrontend, workbook:Workbook) = {
  //get profit loss data list from uploaded excel
  var rowKey_profit_loss: ListMap[String, String] = RowKeys.rowKeys_ProfitLoss.map(key_val => (key_val._2.toLowerCase, key_val._1))
  val profitLossSheet = workbook.getSheet("P&L")
  val (profitLossStartRow: Int, profitLossStartColumn: Int) = ExcelReader.findExcelRowAndColumnWithCellValue(profitLossSheet, "Net Sales", true)
  val (profitLossEndRow: Int, _: Int) = ExcelReader.findExcelRowAndColumnWithCellValue(profitLossSheet, "Net Income", true)
  var profitLossDataList = new ListBuffer[ProfitLossCol]()

  var netProfitLoss = "0";
  var capitalGain = "0"
  for (rowNum <- profitLossStartRow to profitLossEndRow) {
    val row = profitLossSheet.getRow(rowNum)
    val excelEnglishKey = ExcelReader.getCellValueAsStringOption(row, profitLossStartColumn).getOrElse("").toLowerCase;
    val keyOpt = rowKey_profit_loss.get(excelEnglishKey)
    if (keyOpt.isDefined) {
      profitLossDataList += ProfitLossCol(keyOpt.get, ExcelReader.getCellValueAsStringOption(row, profitLossStartColumn + 1).getOrElse(""))

      //set oridinary earnings
      if (keyOpt.get.equals("net_income")) {
        netProfitLoss = ExcelReader.getCellValueAsStringOption(row, profitLossStartColumn + 1).getOrElse("0")
      }
      else if (keyOpt.get.equals("including_realized_gain_on_disposal_of_assets_held_for_more_than_one_year")) {
        capitalGain = ExcelReader.getCellValueAsStringOption(row, profitLossStartColumn + 1).getOrElse("0")
      }
    }
  }
  var ordianaryOptionFromFrontEnd = Json.parse(frontEndFinancialData.ordinaryearningsandgain.getOrElse(companyfinancialdataServiceRead.ordinaryearningsandgain)).as[OrdinaryEarningsAndGainOption]
  val ordinaryearningsandgain = "{\"netprofitloss\":" + netProfitLoss + ",\"capitalgain\":" + capitalGain + ",\"currency\":" + ordianaryOptionFromFrontEnd.currency.getOrElse("1") + "}"
  (profitLossDataList, ordinaryearningsandgain)
}
