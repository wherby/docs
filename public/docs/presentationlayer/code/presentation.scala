case class OutPutParam(sheetName: String,
                       columnList: Seq[ColumnWithType],
                       sheetContent: () => Seq[Map[String, String]],
                       startWritingRow: Int)

case class ColumnWithType(columnName:String,
                          columnType:ExcelColumnFormat = ExcelColumnFormat.format_string,
                          notfoundValue:String="")

object ExcelColumnFormat extends Enumeration {
  type ExcelColumnFormat = Value
  val user_input =Value("USERVALUE")
  val format_string = Value("STRING")
  val format_date = Value("DATE")
  val format_double = Value("DOUBLE")
  val format_double2d = Value("DOUBLE2D")
  val format_double4d = Value("DOUBLE4D")
  val format_double_without_comma = Value("DOUBLEWITHOUTCOMMA")
    ....
}