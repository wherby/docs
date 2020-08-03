
private def getHashID(securityPrice: SecurityPrice):String={
  val strToHash:String = securityPrice.gsp+ securityPrice.isin +securityPrice.pricetype + securityPrice.source+
    securityPrice.exchange + securityPrice.pricedate + securityPrice.cname +securityPrice.currency +securityPrice.assetclass
  MessageDigest.getInstance("MD5").digest(strToHash.getBytes).map(0xFF & _).map { "%02x".format(_) }.foldLeft(""){_ + _} +
    securityPrice.pricedate + securityPrice.cname
}


override def recordMultiple(securityPriceList:Seq[SecurityPrice])={
  val idChangedList = securityPriceList.map{
    item=> val newID = getHashID(item)
      item.copy(id = newID)
  }.groupBy(_.id).map(_._2.head).toSeq
  val seqRecord = idChangedList.map(entityToRow)
  val ids = idChangedList.map(_.id)
  val idExisted= Await.result( db.run(Securityprices.filter(_.id.inSet(ids)).result).map(seq =>seq.map(_.id))
    .map(seq=>seq.toSet), 3 seconds)
  println(idExisted)
  val newSecurity = seqRecord.filterNot(item=>idExisted.contains(item.id))
  db.run(Securityprices++= newSecurity)
}
