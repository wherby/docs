import scala.concurrent.Future

def fiHasInvestments(fiIds: String) = deadbolt.Pattern(value = "v_myfi", patternType = PatternType.REGEX)() { implicit request =>
  validateUserUtil.validateUser(request, fiIds.split(",").seq).flatMap(_ => {
    fiuserServiceRead.fiHasInvestments(fiIds).map {
      fiHasInvestments => Ok(Json.toJson(fiHasInvestments))
    }
  }).recover {
    case e: UnauthorizedException => Unauthorized
  }
}

def validateUser(request: AuthenticatedRequest[Any], ids: Seq[String]): Future[Unit] = {
  request.subject.map(s => {
    val isPwC = s.permissions.filter(_.value == "b_pwc").nonEmpty
    if (!isPwC) {
      userRead.getUser(Some(s.identifier)).map(u => {
        ids.foreach(entityId=>{
          val validuser = u.map(_.userid.exists(_.equals(entityId))).getOrElse(false)
          if (!validuser) throw new UnauthorizedException()
        })
      })
    } else {
      Future.unit
    }
  }).getOrElse(Future.failed(new UnauthorizedException()))
}