def queryForApprover(lastSendTimestampString: String)={
  var  notificationFilter = Map[String,String]()
  notificationFilter +=("notificationType" -> NotificationType.approver.toString)
  notificationFilter +=("timestamp" -> lastSendTimestampString)
  notificationRead.listNotifications(notificationFilter).map{
    notificationSeq => if(notificationSeq.length >0){
      Logger.debug("Send mail to approver")
      sendMailToRole("PwCApprover")
    }
  }
}


def sendMailToRole(roleStr:String)={
  var  userFilter = Map[String,String]()
  userFilter +=("enable" ->"true")
  userFilter += ("role"->roleStr)
  userRead.userFilterQuery(userFilter).map{
    seqUser=> val emailSeq = seqUser.map{
      user => user.email
    }
      emailSeq.map{
        emailTmp =>
          val email = mailSender.getTestEmail(Seq(emailTmp))
          mailSender.sendEmail(email)
      }
      println(s"send mail to $emailSeq")
  }
}