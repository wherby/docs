

override def updateFinancialData(currentUserEmail: String,
                                 subject: Option[Subject],
                                 financialData: FinancialdataGetFromFrontend): Future[Unit] = {
  validateUserUtil.validateExternalUserPermissionForCompany(subject, financialData.companyId, financialData.year).map(_ => {
    ...
    var newFinancedata = waitForUpdateFinancialDataAndGenerateNotification(currentUserEmail, subject, financialData, companyuser)
    ...
  })
}

private def waitForUpdateFinancialDataAndGenerateNotification(userEmail: String,
                                                              subject: Option[Subject],
                                                              financialData: FinancialdataGetFromFrontend,
                                                              companyuser: Option[Companyuser]) = {
  val currentUserPrivileges = subject.map(s => s.permissions.mkString(",")).getOrElse("")
  val newFinancedataAndNotification = checkPermissionThenGetFiDataAndNotification(financialData, userEmail, currentUserPrivileges, companyuser)
  val newFinancedata = newFinancedataAndNotification.financialData
  val newNotifications = newFinancedataAndNotification.notification
  val futureUpdateFinancialdata = update(newFinancedata)
  var resultUpdateFinancialdata = "failed"
  futureUpdateFinancialdata onComplete {
    case util.Success(value) => {
      resultUpdateFinancialdata = "success"
      waitForClearNotifations(newFinancedata.companyId, newFinancedata.year, financialData.operationType)
      notificationWrite.waitForCreateNotifications(newNotifications)
    }
    case scala.util.Failure(exception) => throw new Exception(exception.toString)
  }
  Await.result(futureUpdateFinancialdata, Duration.apply(10.toLong, duration.SECONDS))
  newFinancedata
}