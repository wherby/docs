export const fetchValuationProgress = async (fundsWithStatus: EngamentFundsWithStatus[], setValuationStatus: (fundEngagementWithValuationStatus: FundEngagementWithValuationStatus) => void) => {
  fundsWithStatus.map(fundsWithStatus => {
    var fundEngagementId = fundsWithStatus.fundEngagementId.toString()
    var fundId = fundsWithStatus.fundId

    getSecurityNameList(fundEngagementId).then((value) => {
      if (value.length > 0) {
        var item = {
          fundEngagementId: fundEngagementId,
          fundId: fundId,
          available: true
        } as FundEngagementWithValuationStatus
        setValuationStatus(item)
      } else {
        var item = {
          fundEngagementId: fundEngagementId,
          fundId: fundId,
          available: false
        } as FundEngagementWithValuationStatus
        setValuationStatus(item)
      }
    })
  })
}