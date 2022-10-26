class EhCacheService @Inject()(
                                cache: AsyncCacheApi,
                                sheetTypeReportTypeMapDAO: SheetTypeReportTypeMapDAO,
                              )(implicit ec: ExecutionContext) {
  val OLD_TYPE_IDS = "oldtypeids"
  val NEW_TYPE_IDS = "newtypeids"
  val CONFIRMATION_TYPE_IDS = "confirmationtypeids"
  val POSITION_CACHE_IDS = "positioncacheids"

  def lookupPositionCashSheetTypeIds(): Future[Seq[String]] = {
    ...
  }
}
