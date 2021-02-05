case class ConfirmationRecord(accountNumber:Option[String]=None,
                              balance:Option[String]=None,
                              currency:Option[String]=None,
                              quantity:Option[String]=None,
                              descriptionOrISIN:Option[String]=None,
                              etc:Option[String]=None)
case class ConfirmationRecordWithFile(record:ConfirmationRecord, fileName:String)
case class ConfirmationFile(fileName:String,records:Seq[ConfirmationRecord],date:Option[String])
case class ConfirmationStorage(files:Seq[ConfirmationFile])