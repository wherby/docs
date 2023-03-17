
private def getSecrurityPrivceOrderd(isin: String,currency:String,  pricedate: String, securityType: String, priceAndSource: PriceAndSource): Seq[SecurityPrice] = {
  var continueQuery = true
  val sourceFrom = securityType match {
    case "Standard" => priceAndSource.standard
    case "Future" => priceAndSource.future
    case "Option" => priceAndSource.option
    case _ => priceAndSource.other
  }
  var result: Seq[SecurityPrice] = Seq()
  val res2 = getByIsinAndDate(isin, currency, pricedate)
  if(res2.map(_.exchange).toSet.toSeq.length ==1){
    result =res2
  }else
  {
    var priceType = ""
    var source = ""
    for (priceType <- priceAndSource.priceOrder) {
      for (source <- sourceFrom) {
        if (continueQuery && priceType.length>0  && source.length >0) {
          val ret = getSecurityPriceByPriceAndOrder(isin, pricedate, priceType, currency,source)
          if (ret.length > 0) {
            continueQuery = false
            result = ret
          }
        }
      }
    }
  }
  result
}