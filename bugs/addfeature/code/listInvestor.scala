import scala.concurrent.Future

def queryPage(page: Int, pageSize: Int, name: Option[String], country: Option[String], city: Option[String], sorterfield: Option[String], sorterorder: Option[String], investorType: Option[String], includeInActive: Option[Boolean]) =
  deadbolt.Pattern(value = "(v_pwc)|(v_myfi)", patternType = PatternType.REGEX)() { implicit request => {
  ParameterCheck.pageChecker(page,pageSize)
  var filterMap = Map[String, String]()
  name.map(someName => filterMap += ("name" -> someName))
  country.map(someCountry => filterMap += ("country" -> someCountry))
  city.map(someCity => filterMap += ("city" -> someCity))
  investorType.map(someType => filterMap += ("investorType" -> someType))
  includeInActive.map(include => filterMap += ("includeInActive" -> include.toString))
  investorServiceRead.listPage(page, pageSize, filterMap, sorterfield, sorterorder).map(page => {
    Ok(Json.obj("investors" -> Json.toJson(page.items), "total" -> page.total))
  })
}
}