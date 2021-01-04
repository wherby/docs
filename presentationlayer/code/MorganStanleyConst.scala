
trait EGAColConstMorgenStanley extends EGAColConstBase {
  override val positionColumnList = replaceColumn(EGAColConstBase.positionColumnList, "Group1", ColumnWithType("Group1", ExcelColumnFormat.format_formula))

  override val GLColumnList = replaceColumn(EGAColConstBase.GLColumnList, "EndingBalanceAmount", ColumnWithType("EndingBalanceAmount", ExcelColumnFormat.format_formula))
}

