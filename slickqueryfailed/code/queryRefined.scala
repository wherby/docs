def queryPageWithName(page: Int, pageSize: Int, useridOpt: Option[String], engagementidOpt: Option[String], roleOpt: Option[String]) =
  deadbolt.Pattern()() {implicit authRequest =>
    var filterMap = Map[String, String]()
      ...
    engageuserRead.listPageWithEngagement(page,pageSize,filterMap).map{
      page=>val engageWithName= page.items.map{
        item=> EngageuserWithName(item._1,item._2)
      }
        Ok(Json.obj("engageuserWithName" -> Json.toJson(engageWithName), "total" -> page.total))
    }
  }