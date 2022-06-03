import scala.concurrent.{Await, Future, duration}
import scala.concurrent.duration.Duration

def lookup(companyId:String, year:Int, reCalculate:Boolean=true) = deadbolt.Pattern(value = "(v_pwc)|(v_mycompany)|(v_myfi)|(v_directinvestor)", patternType = PatternType.REGEX)() { implicit request =>
{
  validateUserUtil.validateExternalUserPermissionForCompany(request.subject,companyId,year).flatMap(_=>{
    (if(reCalculate){
      recalculate(companyId,year)
    }else{
      companypfictestServiceRead.lookup(companyId, year)
    }).map(
      pficTest => {
        pficTest match {
          case Some(value) => Ok(Json.toJson(parseDataToFrontend(value)))
          case None => Ok("")
        }
      }
    )
  }).recover {
    case e: UnauthorizedException => Unauthorized
  }
}
}

def validateExternalUserPermissionForCompany(subject:Option[Subject], companyId: String, year:Int): Future[Any] = {
  Future(subject.map(s => {
    val isFi = s.permissions.filter(_.value == "b_myfi").nonEmpty
    val isDirectInvestor = s.permissions.filter(_.value == "b_directinvestor").nonEmpty
    val isCompany = s.permissions.filter(_.value == "b_mycompany").nonEmpty
    val isPwC = s.permissions.filter(_.value == "b_pwc").nonEmpty

    val futureLookupUser = userRead.getUser(Some(s.identifier))
    var resultLookupUser = "failed"
    futureLookupUser onComplete {
      case util.Success(value) => resultLookupUser = "success"
      case scala.util.Failure(exception) => throw new UnauthorizedException()
    }
    val userGot = Await.result(futureLookupUser, Duration.apply(10.toLong,duration.SECONDS))

    if(!isPwC){
      if (isFi) {
        val futureLookupInvestment = fiCompanyInvestmentServiceRead.listByCompany(companyId,year)
        var resultLookupInvestment = "failed"
        futureLookupInvestment onComplete {
          case util.Success(value) => resultLookupInvestment = "success"
          case scala.util.Failure(exception) => throw new UnauthorizedException()
        }
        val Investments= Await.result(futureLookupInvestment, Duration.apply(10.toLong,duration.SECONDS))

        userGot.map(user => {
          val idList = Investments.map(investment => investment.fiId)
          val hasPermission = user.userid.map(useridSingle => idList.contains(useridSingle)).reduce(_ || _)
          if(!hasPermission) throw new UnauthorizedException()
          else Future.unit
        })
      } else if (isDirectInvestor){
        val futureLookupInvestment = directInvestorInvestmentServiceRead.listByCompany(companyId,year)
        var resultLookupInvestment = "failed"
        futureLookupInvestment onComplete {
          case util.Success(value) => resultLookupInvestment = "success"
          case scala.util.Failure(exception) => throw new UnauthorizedException()
        }
        val Investments= Await.result(futureLookupInvestment, Duration.apply(10.toLong,duration.SECONDS))
        userGot.map(user => {
          val idList = Investments.map(investment => investment.investorId)
          val hasPermission = user.userid.map(useridSingle => idList.contains(useridSingle)).reduce(_ || _)
          if(!hasPermission) throw new UnauthorizedException()
          else Future.unit
        })
      } else if (isCompany){
        val validuser = userGot.map(_.userid.exists(_.equals(companyId))).getOrElse(false)
        if (!validuser) throw new UnauthorizedException()
        else Future.unit
      }else {
        Future.unit
      }
    }
    else {
      Future.unit
    }
  }).getOrElse(Future.failed(new UnauthorizedException())))
}