private def getByIsinAndDate(isin: String,currency:String,  pricedate: String) = {
  val securityPriceQuery = SecurityPriceQuery(None, None, Some(isin), None, None, None, None,Some(currency), pricedate)
  Await.result(securityPriceRead.query(securityPriceQuery), 1 second)
}

private def getSecurityPriceByPriceAndOrder(isin: String, pricedate: String, priceType: String, currency:String, source: String): Seq[SecurityPrice] = {
  val securityPriceQuery = SecurityPriceQuery(None, None, Some(isin), None, Some(priceType), Some(source), None, Some(currency),  pricedate)
  Await.result(securityPriceRead.query(securityPriceQuery), 1 second)
}

case class SecurityPriceQuery(cname: Option[String], gsp: Option[String], isin: Option[String], assetclass: Option[String],
                              pricetype: Option[String], source: Option[String], exchange: Option[String], currency: Option[String],pricedate: String)