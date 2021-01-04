
object EGAConstMorganStanley {
  val positionColumnList = EGAConstBase.replaceColumn( EGAConstBase.positionColumnList,"Group1",ColumnWithType("Group1",ExcelColumnFormat.format_formula))

  val cashColumnList = EGAConstBase.cashColumnList

  val PSRColumnList = EGAConstBase.PSRColumnList

  val TBColumnList = EGAConstBase.TBColumnList

  val GLColumnList = EGAConstBase.replaceColumn(EGAConstBase.GLColumnList,"EndingBalanceAmount",ColumnWithType("EndingBalanceAmount",ExcelColumnFormat.format_formula))

  val dividendColumnList = EGAConstBase.dividendColumnList

  val RGLColumnList = EGAConstBase.RGLColumnList
}
