case class FinancialdataGetFromFrontend(id: Long, year: Int, companyId: String,currencyId: String, assetdata: Option[String] = None,
                                        liabilitydata: Option[String] = None, profitlossdata: Option[String] = None, fmvdata: Option[String] = None, ordinaryearningsandgain: Option[String] = None,
                                        operationType:String, //The operation type will find the
                                        msg:Option[String], owAssetdata: Option[String] = None, owLiabilitydata: Option[String] = None,
                                        owProfitlossdata: Option[String] = None,incorporationdate: Option[String] = None,  uploadFilesByExternal: Option[Boolean] = None)


case class NextFinancialDataAndNotification(financialData:Financialdata,notification:Seq[Notification])

case class Notification(var id: String, notificationType: NotificationType.NotificationType, notificationDataType: NotificationDataType.NotificationDataType,
                        description: Option[NotificationDescription], status: Boolean, entityname: Option[String],
                        link: String, year: Int, triggerAt: java.sql.Timestamp, triggerBy: Option[String])