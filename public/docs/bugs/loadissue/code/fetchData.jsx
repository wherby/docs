  fetchData = () => {
    this.cancelSingleEGAPolling()
    Promise.all([queryFundsByEngagementIdAndFundAdminType(this.props.engagementId, FrontEndBackEndFundAdminTypeMapping[this.state.fundAccountingType]), queryFundWithStatusByEngagementId(this.props.engagementId), getConfirmationCompletionRatioByEngagementId(this.props.engagementId)])
      .then(([funds, fundsWithStatus, confirmationCompletionRatio]) => {
        this.setState({
          funds: funds,
          fundItemCollapsed: funds.map(fund => { return { id: fund.id, collapsed: true } }),
          fundEngagementsWithStatus: fundsWithStatus,
          fundEngagementWithConfirmationStatus: confirmationCompletionRatio,
          //bookmark
          loading: false,
        }, this.beginSingleEGAPolling)

        getValuationUploadOrNot(fundsWithStatus, (valuationProgress: FundEngagementWithValuationStatus[]) => {
          this.setState(
            prevState => {
              return {
                ...prevState,
                fundEngagementWithValuationStatus: valuationProgress
              }
            })
        })

      }).catch(res => {
        PopErrors(res.toString() + " fetching funds data error")
        this.setState({
          loading: false
        })
      })
  }