import scala.collection.Map

def ruleMatchProcess(
                      reportMap: Map[EGATab, (String, Seq[(String, Seq[MatchRule])])],
                      fundEngagementId: String,
                      matchLogic: (MatchRule, EGATab) => Boolean
                    ): Option[String] = {
  val selectedTypeId = getSelectedReportTypeId(fundEngagementId)
  reportMap.foreach { case (reportType, config) =>
    if (
      Option(config._2).isDefined && Option(config._2).nonEmpty && selectedTypeId.contains(
        config._1
      )
    )
      config._2.foreach(processor_rule => {
        if (processor_rule._2.exists(rule => matchLogic(rule, reportType)))
          return Some(processor_rule._1)
      })
  }
  None
}
