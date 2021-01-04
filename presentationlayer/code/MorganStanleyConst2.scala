
trait EGAColConstMorgenStanley extends EGAColConstBase {
  override val positionColumnList = replaceColumn(positionColumnList, "Group1", ColumnWithType("Group1", ExcelColumnFormat.format_formula))

  override val GLColumnList = replaceColumn(GLColumnList, "EndingBalanceAmount", ColumnWithType("EndingBalanceAmount", ExcelColumnFormat.format_formula))
}

