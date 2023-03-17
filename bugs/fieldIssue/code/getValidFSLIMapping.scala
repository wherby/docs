import java.sql.Timestamp

import scala.concurrent.Future

def getValidFSLIMappingFromFrontData(
                                      fundEngagementId: String,
                                      frontData: Seq[SingleEGAFSLIMappingItem]
                                    ) = {
  val now         = DateTime.now(DateTimeZone.UTC)
  val editTime    = new Timestamp(DateTime.now.getMillis)
  val editTimeStr = now.toString("yyyy-MM-dd HH:mm")+"Z"
  val futureUpdates = fundEngagementRead
    .lookup(fundEngagementId)
    .flatMap(maybeFE => {
      maybeFE match {
        case Some(fE) => {
          val currentSingleEGAFSLIMapping = Json
            .parse(fE.fsli_mapping.getOrElse("[]"))
            .as[Seq[SingleEGAFSLIMappingItem]]

          val fund = fundEngagementRead
            .getFundFromRecord(fE.fundRecord)

          val fundAdminId = fund.map(_.fundAdminId).flatten
          val maybeEngagement = engagementRead.lookup(fE.engagementid)

          val futureFundAdminType = fundAdminId match {
            case Some(fAId) => {
              SafeLogger.logString(Logger.apply(this.toString),"Fund Admin Id: "+fAId)
              fundAdminsRead
                .lookup(fAId)
                .map(mayFundAdmin => {
                  mayFundAdmin.map(fundAdmin => {
                    SafeLogger.logString(Logger.apply(this.toString),"Fund Admin Type: "+fundAdmin.fundAdminType)
                    fundAdmin.fundAdminType
                  })
                })
            }
            case None => {
              SafeLogger.logString(Logger.apply(this.toString),"Fund Admin Id Not Found")
              Future(None)
            }
          }



          val currentStandardItemWithType = futureFundAdminType.flatMap(maybeFundAdminType => {
            maybeFundAdminType match {
              case Some(fundAdminType) => {
                fsliMappingRead
                  .getStandardItemsByFundAdminType(FundAdminType.withName(fundAdminType))
              }
              case None => {
                val empty: Map[String, FSLIMappingStandardItem] = Map()
                Future(empty)
              }
            }
          })

          val updates = for{
            standardMap <- currentStandardItemWithType
            engagement <- maybeEngagement
          }yield{
            if (standardMap.size > 0) {
              val standardItems = standardMap.values.toSeq
              val standardNames = standardItems.map(_.accountname)
              SafeLogger.logString(Logger.apply(this.toString),"Standard item names: "+standardNames.toString)
              val fundAdminType = standardItems(0).fundadmintype
              val lastUpdatedBy = standardItems(0).last_updated_by
              //val id = standardItems(0).id
              /** Make sure every new mapping belongs to the fund admin type
               */
              if (frontData.map(_.pwcMapping).exists(standardNames.contains(_))) {

                /** data to be update in database table
                 */
                val validFrontData =
                  frontData.filter(row => standardNames.contains(row.pwcMapping))
                SafeLogger.logString(Logger.apply(this.toString),"Valid item names: "+validFrontData.map(i=>(i.accountName,i.pwcMapping,i.standardItemId)).toString)
                val invalidFrontData =
                  frontData.filter(row => { !standardNames.contains(row.pwcMapping) })
                SafeLogger.logString(Logger.apply(this.toString),"Valid item names: "+invalidFrontData.map(i=>(i.accountName,i.pwcMapping,i.standardItemId)).toString)
                val frontDataWithId = validFrontData.map(row => {
                  row.copy(standardItemId =
                    standardItems.filter(_.accountname == row.pwcMapping).headOption.get.id
                  )
                })
                val newMapping = frontDataWithId.map(item => {
                  FSLIMapping(
                    "",
                    item.accountName,
                    item.standardItemId,
                    Some(editTime),
                    fundAdminType,
                    lastUpdatedBy
                  )
                })
                val updateSingleEGAFSLIMappingList = currentSingleEGAFSLIMapping.map(item => {
                  frontDataWithId.filter(_.accountName == item.accountName).headOption match {
                    case Some(newItem) => {
                      val maybeStandardItem = standardMap.get(newItem.standardItemId)
                      if (maybeStandardItem.isDefined) {
                        item.copy(
                          standardItemId = maybeStandardItem.get.id,
                          pwcMapping = maybeStandardItem.get.accountname,
                          nature = maybeStandardItem.get.nature,
                          lastEditTime = editTimeStr
                        )
                      } else {
                        item
                      }
                    }
                    case None => {
                      item
                    }
                  }
                })

                val newFund = engagement.get.copy(fsliMapping = Some(Json.toJson(updateSingleEGAFSLIMappingList).toString()))

                Some(
                  (
                    newFund,
                    fE.copy(fsli_mapping =
                      Some(Json.toJson(updateSingleEGAFSLIMappingList).toString())
                    ),
                    validFrontData.length,
                    invalidFrontData.length,
                    newMapping
                  )
                )
              } else {
                None
              }
            } else {
              None
            }
          }
          updates
        }
        case None => Future(None)
      }
    })
  futureUpdates
}