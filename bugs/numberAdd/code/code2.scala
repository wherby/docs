if(parentInvesment.companyId == company.id || companyInvestment.data.flatMap(_.valueOfShares).isDefined){ //parentInvestment uses this company id, means this is parentCompany, so show separate value of shares.
  firstValueOfShares = PficReportCalculator.formatDecimalIntoIntString(Some(investorEndingCommitment * companyInvestment.data.flatMap(_.valueOfShares).getOrElse(BigDecimal(0))), "0")
  secondValueOfShares = PficReportCalculator.formatDecimalIntoIntString(Some(investorEndingCommitment * companyInvestment.data.flatMap(_.secondValueOfShares).getOrElse(BigDecimal(0))), "0")
}
else {
  val totalValueOfShares = (parentInvesment.data.flatMap(_.valueOfShares).getOrElse(BigDecimal(0)) * investorEndingCommitment).setScale(0, BigDecimal.RoundingMode.HALF_UP) +
    (parentInvesment.data.flatMap(_.secondValueOfShares).getOrElse(BigDecimal(0)) *investorEndingCommitment).setScale(0, BigDecimal.RoundingMode.HALF_UP)
}