/**
 * get cell value as string base on cell type
 * @param row
 * @param index
 * @return
 */
def getCellValueAsStringOption(row: Row,index: Int):Option[String] = {
  getCellOption(row, index).flatMap(cell => {
    var cellStyle = cell.getCellType
    cellStyle match {
      case CellType.NUMERIC => {
        Option(cell.getNumericCellValue.toString.trim)
      }
      case CellType.STRING => {
        Option(cell.getStringCellValue.trim)
      }
      case CellType.BOOLEAN => {
        Option(cell.getBooleanCellValue.toString.trim)
      }
      case CellType.FORMULA => {
        cell.getCachedFormulaResultType match {
          case CellType.NUMERIC =>
            Option(cell.getNumericCellValue.toString.trim)
          case CellType.STRING  =>
            Option(cell.getStringCellValue.trim)
          case _=>
            Option(cell.getCellFormula)
        }
      }
      case _ => {
        None
      }
    }
  })
}
