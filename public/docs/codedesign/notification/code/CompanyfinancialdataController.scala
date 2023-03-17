import scala.concurrent.{Await, Future}

def upload(companyId: String, year: Int, operationType: String) = deadbolt.Pattern(value = "(v_pwc)|(v_mycompany)", patternType = PatternType.REGEX)(parse.multipartFormData(maxLength = 1024*1024))  { implicit request =>
  request.body.file("file").map { file =>
    try{
      ...
        companyfinancialdataServiceWrite.updateFinancialData(request.session.get("email").getOrElse(""), request.subject, financialData).map(res => {Ok("success")})
    }
}