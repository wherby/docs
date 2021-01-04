
trait EGAColConstBase {
  def replaceColumn(orgSeq:Seq[ColumnWithType], columnName:String, newFormat:ColumnWithType)={
    orgSeq.map{
      col =>col.columnName match {
        case x if x == columnName => newFormat
        case _=>col
      }
    }
  }
  val positionColumnList = EGAColConstBase.positionColumnList

  val cashColumnList =  EGAColConstBase.cashColumnList

  val PSRColumnList = EGAColConstBase.PSRColumnList

  val TBColumnList = EGAColConstBase.TBColumnList

  val GLColumnList = EGAColConstBase.GLColumnList

  val dividendColumnList = EGAColConstBase.dividendColumnList

  val RGLColumnList = EGAColConstBase.RGLColumnList

}
