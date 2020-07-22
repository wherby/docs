def excelToArray(filePath: String) :Seq[Seq[ExcelField]]={
    ...
  while (rowIterator.hasNext){
    val rowT = rowIterator.next()
    var rowSeq:Seq[ExcelField]=Seq()
    val Numbers = rowT.getLastCellNum()
    var i =0
    while(i< Numbers){
      val cellT = rowT.getCell(i)
      rowSeq =rowSeq :+ getCellValue(cellT)
      i = i+1
    }
    res =res :+rowSeq
  }
  removeEmptyEnd(res)
}

def getCellValue(cell: Cell):ExcelField={
  try {
    val decimalFormat: DecimalFormat = new DecimalFormat("0.0000")
    val dateFormat = new SimpleDateFormat("yyyy/MM/DD");
    cell.getCellType match {
      case CellType.BOOLEAN => ExcelField(ExcelFieldType.BoolType, cell.getBooleanCellValue.toString)
      case CellType.NUMERIC => if (DateUtil.isCellDateFormatted(cell)) {
        ExcelField(ExcelFieldType.DateType, dateFormat.format(cell.getDateCellValue))
      } else {
        try {
          ExcelField(ExcelFieldType.NumberType, decimalFormat.format(cell.getNumericCellValue))
        } catch {
          case e: Exception =>
            ExcelField(ExcelFieldType.Unknown, cell.getStringCellValue)
        }

      }
      case CellType.STRING => ExcelField(ExcelFieldType.StringType, cell.getStringCellValue.trim)
      case CellType.FORMULA => try {
        ExcelField(ExcelFieldType.NumberType, decimalFormat.format(cell.getNumericCellValue))
      } catch {
        case e: Exception =>
          ExcelField(ExcelFieldType.Unknown, cell.getCellFormula)
      }

      case _ => ExcelField(ExcelFieldType.Unknown, cell.getStringCellValue)
    }
  }catch {
    case e:Exception =>
      ExcelField(ExcelFieldType.Unknown,"")
  }
}