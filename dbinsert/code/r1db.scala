override def recordMultiple(securityPriceList:Seq[SecurityPrice])={
  val seqRecord = securityPriceList.map(insertOrUpdate)
  db.run( DBIO.sequence(seqRecord))
}

def insertOrUpdate(securityPrice: SecurityPrice)={
  val recordOption= Securityprices.filter(_.cname ===securityPrice.cname)
    .filter(_.isin === securityPrice.isin)
    .filter(_.gsp === securityPrice.gsp)
    .filter(_.pricedate === securityPrice.pricedate)
    .filter(_.assetclass === securityPrice.assetclass)
    .filter(_.exchange ===securityPrice.exchange)
    .filter(_.source ===securityPrice.source)
    .filter(_.pricetype === securityPrice.pricetype)
    .filter(_.currency === securityPrice.currency).result.headOption
  for{
    existing<-recordOption
    row = existing.map{price=>entityToRow(securityPrice).copy(id = price.id)}.getOrElse(entityToRow(securityPrice))
    result <-Securityprices.insertOrUpdate(row)
  }yield result
}
