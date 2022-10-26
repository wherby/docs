package com.pwc.ds.awm.strategy.fundadmin

trait FundReportStrategyFactory {

  def getFundReportStrategy(name: String): Option[FundReportStrategy]
}
