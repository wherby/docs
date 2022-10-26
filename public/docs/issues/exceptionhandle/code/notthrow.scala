import scala.concurrent.Future

def queryFundsWithStatus(engagementId: String) = deadbolt.SubjectPresent()() { implicit request =>
{
  val email = request.session.get("email").getOrElse("")
  userInfoQueryService.verifyEmailWithEngagement(email,engagementId).flatMap{
    verified=>verified match {
      case  true =>fundsRead.queryFundReportStatus(engagementId).map(fe => Ok(Json.toJson(fe)))
      case _=>Future(Unauthorized)
    }
  }
}
}

def verifyEmailWithEngagement(email:String,engagementId: String):Future[Boolean]={
  engagementRead.getAssignedEngagements(email).map{
    engagementList=>engagementList.map(_.id).contains(engagementId)
  }
}