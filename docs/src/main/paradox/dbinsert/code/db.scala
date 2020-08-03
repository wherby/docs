override def create(securiryPrice: SecurityPrice): Future[Int] = {
  db.run(
    Securityprices += entityToRow(securiryPrice)
  )
}

override def update(securityPrice: SecurityPrice): Future[Int] = {
  db.run(
    queryById(securityPrice.id).update(entityToRow(securityPrice))
  )
}