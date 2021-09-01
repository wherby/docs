
object BasicExcelReaderHelper {
  def processFileOnExcelReader(
                                filePath: String,
                                fundName: String,
                                excelReader: BasicExcelReader
                              ): Seq[Map[String, String]] = {
    val reader       = excelReader.readerOpt.getOrElse(defaultExcelReaderForExcel(excelReader.header))
    val excelContent = reader(filePath)
    val targetContent = excelReader.filter.foldLeft(excelContent) { (excelContent, filter) =>
      excelContent.filter(row => filter(row, fundName))
    }
    targetContent.map { row =>
      excelReader.mapper(row)
    }
  }

  def defaultExcelReaderForExcel(header: String): String => Seq[Seq[ExcelField]] =
    (filePath: String) => {
      val allSheet         = ExcelToArray.multiSheetToArray(filePath)
      val headerNormalized = ExcelReaderHelper.stringNormalizer(header)
      ExcelReaderHelper.getSpecifiedSheetWithoutReplacement(allSheet, headerNormalized)
    }

  def basicRowLengthFilter(length: Int) =
    (row: Seq[ExcelField], fundName: String) => {
      row.length >= length
    }

  def basicHeaderTitleFilter(index: Int, value: String) =
    (row: Seq[ExcelField], fundName: String) => {
      row(index).field.toLowerCase.trim != value.toLowerCase.trim
    }

  def basicEmptyFilter(index: Int) = (row: Seq[ExcelField], fundName: String) => {
    row(index).field.trim.length > 0
  }
}

case class BasicExcelReader(
                             header: String,
                             filter: Seq[(Seq[ExcelField], String) => Boolean],
                             mapper: Seq[ExcelField] => Map[String, String],
                             readerOpt: Option[String => Seq[Seq[ExcelField]]]
                           )
