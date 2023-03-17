
def excelToArray(filePath: String) :Seq[Seq[ExcelField]]={
    ...
  while (rowIterator.hasNext){
    val rowT = rowIterator.next()
    var rowSeq:Seq[ExcelField]=Seq()
    val cellInterator = rowT.cellIterator()
    while(cellInterator.hasNext){
      val cellT = cellInterator.next()
      rowSeq =rowSeq :+ getCellValue(cellT)
    }
    res =res :+rowSeq
  }
}

def getCellValue(cell: Cell):ExcelField={
  val decimalFormat: DecimalFormat = new DecimalFormat("0.##")
  cell.getCellType match {
    case CellType.BOOLEAN => ExcelField(ExcelFieldType.BoolType, cell.getBooleanCellValue.toString)
    case CellType.NUMERIC => if(DateUtil.isCellDateFormatted(cell)){
      ExcelField(ExcelFieldType.DateType,cell.getDateCellValue.toString)
    }else{
      try {
        ExcelField(ExcelFieldType.NumberType,decimalFormat.format(cell.getNumericCellValue))
      }catch {
        case e:Exception =>
          ExcelField(ExcelFieldType.Unknown,cell.getStringCellValue)
      }

    }
    case CellType.STRING => ExcelField(ExcelFieldType.StringType, cell.getStringCellValue.trim)
    case CellType.FORMULA => try{
      ExcelField(ExcelFieldType.NumberType, decimalFormat.format( cell.getNumericCellValue))
    }catch {
      case e:Exception =>
        ExcelField(ExcelFieldType.Unknown,cell.getCellFormula)
    }

    case _=>ExcelField(ExcelFieldType.Unknown,cell.getStringCellValue)
  }
}