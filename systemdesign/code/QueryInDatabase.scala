override def query(securityPriceQuery: SecurityPriceQuery): Future[Seq[SecurityPrice]] = {
  val query = Securityprices.filter(securityPrice => securityPriceQuery.cname.map(cname => securityPrice.cname === cname).getOrElse(true: Rep[Boolean]))
    .filter(securityPrice => securityPriceQuery.gsp.map(gsp => securityPrice.gsp === gsp).getOrElse(true: Rep[Boolean]))
    .filter(securityPrice => securityPriceQuery.isin.map(isin => securityPrice.isin === isin).getOrElse(true: Rep[Boolean]))
    .filter(securityPrice => securityPriceQuery.assetclass.map(assetclass => securityPrice.assetclass === assetclass).getOrElse(true: Rep[Boolean]))
    .filter(securityPrice => securityPriceQuery.pricetype.map(priceType => securityPrice.pricetype like(s"%$priceType%")).getOrElse(true: Rep[Boolean]))
    .filter(securityPrice => securityPriceQuery.source.map(source => securityPrice.source === source).getOrElse(true: Rep[Boolean]))
    .filter(securityPrice => securityPriceQuery.exchange.map(exchange => securityPrice.exchange === exchange).getOrElse(true: Rep[Boolean]))
    .filter(securityPrice => securityPriceQuery.currency.map(currency => securityPrice.currency === currency).getOrElse(true: Rep[Boolean]))
    .filter(securityPrice => securityPrice.pricedate === securityPriceQuery.pricedate)
  db.run(query.result).map {
    maybeRow => maybeRow.map(rowToEntity)
  }
}