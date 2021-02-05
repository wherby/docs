def invFilter(securityItems:Seq[SecurityItem])(accounts: Seq[AccountRecord])(row: Map[String, String])(record: ConfirmationRecord) = {
  val accountNumber = accounts.filter(acc => acc.account == row.get(WorkInvColumns.CUSTODIAN).getOrElse("")).headOption.map(_.accountNumber).getOrElse("")
  val isinStr= securityItems.filter(item=> item.cname == row.getOrElse(WorkInvColumns.DESCRIPTION, ""))
    .headOption.map(_.isin).getOrElse("").trim
  equalString(accountNumber,record.accountNumber.getOrElse("")) &&
    equalString(isinStr,record.descriptionOrISIN.getOrElse("")) &&
    equalString(row.getOrElse(WorkInvColumns.ORI_CCY_RATE, ""), record.currency.getOrElse(""))
}

def equalString(a:String,b:String):Boolean={
  a.trim==b.trim && a.length>0
}