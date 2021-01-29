object FundC {
  val thisfund =""

  def belongsToFund(name: String) = {
    belongToSomeSelectedFunds(name, thisfund)
  }

  def cashFilter(accounts: Seq[AccountRecord])(row: Map[String, String])(record: ConfirmationRecord) = {
    val accountNumber = accounts.filter(acc => acc.account == row.get(WorkCashColumns.GL_ACCOUNT_NUMBER)).headOption.map(_.accountNumber).getOrElse("")
    row.getOrElse(WorkCashColumns.BANK_ACCOUNT_CURRENCY, "") == record.currency &&
      accountNumber == record.accountNumber
  }

  def cashGet(record: ConfirmationRecordWithFile): Map[String, String] = {
    Map(WorkCashColumns.BANK_ACCOUNT_NUMBER -> record.record.accountNumber.getOrElse(""),
      WorkCashColumns.AMOUNT_PER_CONFIRMATION -> record.record.balance.getOrElse(""),
      WorkCashColumns.QUESTIONARE_FILE -> record.fileName)
  }

  def invFilter(accounts: Seq[AccountRecord])(row: Map[String, String])(record: ConfirmationRecord) = {
    val accountNumber = accounts.filter(acc => acc.account == row.get(WorkInvColumns.CUSTODIAN)).headOption.map(_.accountNumber).getOrElse("")
    accountNumber == record.accountNumber &&
      row.getOrElse(WorkInvColumns.INVEST_ID, "") == record.descriptionOrISIN &&
      row.getOrElse(WorkInvColumns.ORI_CCY_RATE, "") == record.currency
  }

  def invGet(record: ConfirmationRecordWithFile): Map[String, String] = {
    Map(WorkInvColumns.QUESTIONARE_FILE -> record.fileName,
      WorkInvColumns.QUANTITY_PER_CONFIRMATION -> record.record.quantity.getOrElse(""),
      WorkInvColumns.INVESTMENT_VALUE_PER_CONFIRMATION -> record.record.balance.getOrElse(""))
  }


  def generate(cvg: ConfirmationValuationGSP, selections: Seq[FundEngagementReportTypeSelectionData], accountNameNumberMappingParam: Seq[AccountRecord]) = {
    val cashConfig1 = FinderConfig("111", cashFilter(accountNameNumberMappingParam), cashGet)
    val invConfig1 = FinderConfig("222", invFilter(accountNameNumberMappingParam), invGet)
    val bnyFinderConf = FundFinderConfig(belongsToFund, Seq(cashConfig1), Seq(invConfig1))


    cvg.copy(workCash = GenericConfirmationFinder.getWorkCash(cvg.workCash, selections, bnyFinderConf),
      workInv = GenericConfirmationFinder.getWorkInv(cvg.workInv, selections, bnyFinderConf))
  }
}