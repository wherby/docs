import scala.concurrent.{ExecutionContext, Future}

class FundEngagementReportTypeSelectionReadImpl @Inject() (
                                                            fundEngagementReportTypeSelectionDAO: FundEngagementReportTypeSelectionDAO,
                                                            sheetTypeReportTypeMapDAO: SheetTypeReportTypeMapDAO,
                                                            ehCacheService: EhCacheService
                                                          )(implicit ec: ExecutionContext)
  extends FundEngagementReportTypeSelectionRead {
  ...
}
