import scala.concurrent.Future

def listPage(pageNum: Int = 0, pageSize: Int = 10, filterMap: Map[String, String], sortField: Option[String], sortOrder: Option[String]): Future[Page[Investoruser]] = {

  val offset = pageSize * (pageNum - 1)

  var filteredQuery = Investorusers.filter { user => filterMap.get("city").map(someCity => user.city.asColumnOf[String] like s"%$someCity%").getOrElse(true: Rep[Boolean]) }
    .filter { user => filterMap.get("name").map(f = someName => user.name like s"%$someName%").getOrElse(true: Rep[Boolean]) }
    .filter { user => filterMap.get("includeInActive").map(includes => {
      if(includes == "true"){
        true: Rep[Boolean]
      }else{
        user.activestatus === true
      }
    }).getOrElse(user.activestatus === true)} // not include inactive investor user by default

  if(filterMap.get("investorType").nonEmpty){
    var investorType = InvestorType.getIntInvestorType(filterMap.get("investorType"))
    filteredQuery = filteredQuery.filter(_.investorType === investorType)
  }

  var oder = sortOrder.map({
    _ match {
      case "ascend" => Ordering(Ordering.Asc)
      case _ => Ordering(Ordering.Desc)
    }
  })
  var columnOrder = ColumnOrdered[String](_: Rep[String], Ordering(Ordering.Asc));
  val sortQuery = sortField match {
    case Some("name") => filteredQuery.sortBy(_.name)(columnOrder)
    case Some("city") => filteredQuery.sortBy(_.city.asColumnOf[String])(columnOrder)
    case _ => filteredQuery.sortBy(_.createdAt.desc)
  }

  var query = sortQuery.drop(offset).take(pageSize)
  for {
    totalRows <- db.run(filteredQuery.length.result)
    result <- db.run(query.result).map(rows => rows.collect { case userRow => convertUseRowToUser(userRow) })
  } yield Page(result, pageNum, offset, totalRows)
}