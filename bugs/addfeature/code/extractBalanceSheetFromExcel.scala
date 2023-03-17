

def extractBalanceSheetFromExcel(workbook: Workbook) = {
  val balanceSheet = workbook.getSheet("Balance Sheet")
  val (assetStartRow: Int, assetStartColumn: Int) = ExcelReader.findExcelRowAndColumnWithCellValue(balanceSheet, "Current assets", true)
  val (assetEndRow: Int, _: Int) = ExcelReader.findExcelRowAndColumnWithCellValue(balanceSheet, "Total assets", true)
    ...
}
