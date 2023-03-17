override def recordMultiple(securityPriceList: Seq[SecurityPrice]): Future[Seq[Int]] = {
  securityPriceDAO.recordMultiple(securityPriceList)
}