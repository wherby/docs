def getEngagementSecurityPrice() = Action.async(trim(parse.json)) { implicit request => {
    ...
  egaRead.lookupByFundEngagementId(fundEngagementId).flatMap {
    egaOpt =>
      egaOpt.flatMap(_.securityItems).map(str => Json.parse(str).as[EngagementSecurity]).map {
        engageSecurity =>
          Future(engageSecurity.items.map {
            securityItem =>
              getSecrurityPrivceOrderd(securityItem.isin,securityItem.currency.getOrElse(""),  date, securityItem.catagory, priceAndSource)
          }.flatten)
      }.getOrElse(Future(Seq()))
  }
}
}