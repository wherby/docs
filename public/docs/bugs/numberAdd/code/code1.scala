if(parentInvesment.companyId == company.id || companyInvestment.data.flatMap(_.valueOfShares).isDefined){ //parentInvestment uses this company id, means this is parentCompany, so show separate value of shares.
  firstValueOfShares = PficReportCalculator.formatDecimalIntoIntString(Some(investorEndingCommitment * companyInvestment.data.flatMap(_.valueOfShares).getOrElse(BigDecimal(0))), "0")
  secondValueOfShares = PficReportCalculator.formatDecimalIntoIntString(Some(investorEndingCommitment * companyInvestment.data.flatMap(_.secondValueOfShares).getOrElse(BigDecimal(0))), "0")
}
else { //it's child company. both value of shares show seenotes, show proRataShareOfShareholder of total parent valueOfShares in the note.
  val totalValueOfShares:BigDecimal = parentInvesment.data.flatMap(_.valueOfShares).getOrElse(BigDecimal(0)) + parentInvesment.data.flatMap(_.secondValueOfShares).getOrElse(BigDecimal(0))
}



def formatDecimalIntoIntString(number: Option[BigDecimal], noneString: String): String = {
  number match {
    case Some(value) => {
      roundUpBigDecimal(value)
    }
    case _ => noneString
  }
}

def roundUpBigDecimal(value: BigDecimal): String = {
  val decimalFormat: DecimalFormat = new DecimalFormat("#,##0")
  var roundUp = value.setScale(0, BigDecimal.RoundingMode.HALF_UP)
  decimalFormat.format(roundUp)
}