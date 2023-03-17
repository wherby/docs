def queryPageWithName(page: Int, pageSize: Int, useridOpt: Option[String], engagementidOpt: Option[String], roleOpt: Option[String]) =
  deadbolt.Pattern()() {implicit authRequest =>
    var filterMap = Map[String, String]()
        ....
    engageuserRead.listPage(page, pageSize, filterMap).flatMap(page => {
      Future.sequence(page.items.map {
        user1 =>
          engagementRead.lookup(user1.engagementid).map {
            engagementOpt =>
              EngageuserWithName(user1, engagementOpt.get)
          }
      }).map {
        engageWithName =>
          Ok(Json.obj("engageuserWithName" -> Json.toJson(engageWithName), "total" -> page.total))
      }
    })
  }