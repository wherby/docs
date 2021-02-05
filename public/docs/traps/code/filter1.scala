def invFilter(securityItems: Seq[SecurityItem])(accounts: Seq[AccountRecord])(row: Map[String, String])(record: ConfirmationRecord) = {
  val accountNumber = accounts.filter(acc => acc.account == row.get(WorkInvColumns.CUSTODIAN)).headOption.map(_.accountNumber).getOrElse("")
  val isinStr = securityItems.filter(item => item.cname == row.getOrElse(WorkInvColumns.DESCRIPTION, ""))
    .headOption.map(_.cname).getOrElse("").trim
  accountNumber == record.accountNumber &&
    isinStr == record.descriptionOrISIN &&
    row.getOrElse(WorkInvColumns.ORI_CCY_RATE, "") == record.currency
}




