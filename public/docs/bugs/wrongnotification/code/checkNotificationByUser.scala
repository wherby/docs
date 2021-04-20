import scala.concurrent.Future
def listNotification(notificationDataType:Option[NotificationDataType.NotificationDataType],year:Option[Int], entityId:Option[String]) = deadbolt.SubjectPresent()(parse.anyContent) {request => {
  ...
  notificationServiceRead.listNotifications(filterMap).flatMap(notifications => {
    if (isPwC || isPwCRE || isPwCAP || isAdmin) {
      notificationServiceRead.filterInternalNotifications(notifications, request.session.get("email").getOrElse(""))
    } else Future(notifications)
  })
}

override def filterInternalNotifications(notifications: Seq[Notification], email: String): Future[Seq[Notification]] = {
  userRead.getUser(Some(email)).flatMap(usero => {
    usero.map(user => {
      userFundServiceRead.lookupFundsByUserId(user.id, Map()).map(funds => {
        notifications.filter(notification => {
          notification.notificationDataType match {
            case NotificationDataType.fiInvestment | NotificationDataType.agreedFiInvestment | NotificationDataType.disagreedFiInvestment |
                 NotificationDataType.externalFiInvestment | NotificationDataType.investorInvestment | NotificationDataType.externalInvestorInvestment |
                 NotificationDataType.agreedInvestorInvestment | NotificationDataType.disagreedInvestorInvestment | NotificationDataType.statementWorkpaper => {
              funds.find(_.id == notification.link).nonEmpty
            }
            case NotificationDataType.financialData | NotificationDataType.agreedFinancialData | NotificationDataType.disagreedFinancialData | NotificationDataType.externalFinancialData | NotificationDataType.externalFundFinancialFile => {
              Await.result(userFundServiceRead.verifyForComapany(email, notification.link, Some(notification.year)), 10 seconds)
            }
            case _ => true
          }
        })
      })
    }).getOrElse(Future(notifications))
  })
}
}
