override def recordSecurityPrice(securityPrice: SecurityPrice): Future[Int] = {
  val queryItem = SecurityPriceQuery(getQueryItem(securityPrice.cname), getQueryItem(securityPrice.gsp), getQueryItem(securityPrice.isin),
    None, getQueryItem(securityPrice.pricetype), getQueryItem(securityPrice.source), getQueryItem(securityPrice.exchange), getQueryItem(securityPrice.currency), securityPrice.pricedate)
  securityPriceDAO.query(queryItem).flatMap {
    securityPriceSeq =>
      securityPriceSeq.headOption match {
        case None => val newRecord = securityPrice.copy(id = UUID.randomUUID().toString)
          securityPriceDAO.create(newRecord)
        case Some(securityPriceOld) => val updateSecurity = securityPrice.copy(id = securityPriceOld.id)
          securityPriceDAO.update(updateSecurity)
      }
  }
}