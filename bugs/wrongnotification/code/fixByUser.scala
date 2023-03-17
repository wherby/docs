import scala.concurrent.Future

def sendMailToRole(roleStr: String, notifications: Seq[Notification]) = {
  var userFilter = Map[String, String]()
  userFilter += ("enable" -> "true")
  userFilter += ("role" -> roleStr)
  userRead.userFilterQuery(userFilter).map {
    seqUser =>
      val emailSeq = seqUser.map {
        user => user.email
      }
      Future.sequence(emailSeq.map {
        emailTmp =>
          val filterdNotificaionF = notificationServiceRead.filterInternalNotifications(notifications, emailTmp)
          filterdNotificaionF.map {
            filterdNotificaion =>
              if (filterdNotificaion.length > 0) {
                val email = mailSender.getTestEmail(Seq(emailTmp))
                mailSender.sendEmail(email)
              }
          }
      })
  }.flatten
}