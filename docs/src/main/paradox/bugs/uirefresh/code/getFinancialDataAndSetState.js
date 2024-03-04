getFinancialDataAndSetState = (companyId, year, refreshedByAuto?: boolean) => {
    this.setState({loading:true})
    let promiseFDataCurrentYear = getFinancialData(companyId, year)
    let promiseFDataLastYear = getFinancialData(companyId, parseInt(year) - 1)
    let promisePficTestData = this.props.dataService.getPficTestData(companyId, year)
    let promiseCurrencyList = getCurrenciesList(year)
    Promise.all([promiseFDataCurrentYear, promiseFDataLastYear, promisePficTestData, promiseCurrencyList]).then(
        ([result, resultLastYear, pficTestData, currencyList]) => {
            if (Object.keys(result).length > 0) {
                var financialData: FinancialData = {
                    assetData: JSON.parse(result.assetdata),
                    liabilityData: JSON.parse(result.liabilitydata),
                    profitLossData: JSON.parse(result.profitlossdata),
                    fmvData: JSON.parse(result.fmvdata),
                    ordinaryEarningsAndGain: JSON.parse(result.ordinaryearningsandgain),
                    owAssetData: JSON.parse(result.owAssetdata),
                    owLiabilityData: JSON.parse(result.owLiabilitydata),
                    owProfitLossData: JSON.parse(result.owProfitlossdata),
                    dataKeys: JSON.parse(result.dataKeys)
                }
                let financialData_LastYear = {} as FinancialData
                let haveData_LastYear = false
                if (Object.keys(resultLastYear).length > 0) {
                    financialData_LastYear = {
                        assetData: JSON.parse(resultLastYear.assetdata),
                        liabilityData: JSON.parse(resultLastYear.liabilitydata),
                        profitLossData: JSON.parse(resultLastYear.profitlossdata),
                        fmvData: JSON.parse(resultLastYear.fmvdata),
                        ordinaryEarningsAndGain: JSON.parse(resultLastYear.ordinaryearningsandgain),
                        owAssetData: JSON.parse(resultLastYear.owAssetdata),
                        owLiabilityData: JSON.parse(resultLastYear.owLiabilitydata),
                        owProfitLossData: JSON.parse(resultLastYear.owProfitlossdata)
                    }
                    haveData_LastYear = true
                }
                var pficTest = {} as any
                if (pficTestData && pficTestData.testresult && currencyList) {
                    pficTest = {
                        currencyName: this.getCurrencyName(result.currencyId, currencyList),
                        isPfic: pficTestData.ispfic,
                        testResult: JSON.parse(pficTestData.testresult),
                        parentCompanyIsPfic: pficTestData.parentCompanyIsPfic
                    }
                    this.setRowKeyDisplay(pficTest.testResult.incomeTestData)
                    this.setRowKeyDisplay(pficTest.testResult.assetTestCostData)
                    this.setRowKeyDisplay(pficTest.testResult.assetTestFMVData)
                    this.setRowKeyDisplay(pficTest.testResult.assetTestFMVDataQuarterly)
                    this.setRowKeyDisplay(pficTest.testResult.publicAssetTestFMVData)
                    setRowDisplayCurrencyForPublicAssetTest(pficTest.testResult.publicAssetTestFMVData, this.getCurrencyName(financialData.fmvData.stock_price_currency_id, currencyList), pficTest.currencyName)
                }
                let newWorkflowState = {
                    modifiedByExternal: result.modifiedByExternal === true,
                    uploadFilesByExternal: result.uploadFilesByExternal === true,
                    internalState: result.internalState,
                    externalState: result.externalState,
                    stateDetail: result.stateDetail,
                    stateChangeLog: [] as StateChangeLog[]
                }
                this.setNonEditableRows(financialData.assetData, BalanceSheetDataRows_asset_subtotal, BalanceSheetDataRows_asset_subtitle);
                this.setNonEditableRows(financialData.liabilityData, BalanceSheetDataRows_liability_subtotal, BalanceSheetDataRows_liability_subtitle);
                this.setNonEditableRows(financialData.profitLossData, ProfitLossDataRows_subtotal, ProfitLossDataRows_subtitle);
                this.setPercentFormatRows(financialData.fmvData.fmv_valuation_and_interest_by_quarter, FMVQuarterlyRows_PercentRow)
                this.setRowKeyDisplay(financialData.assetData)
                this.setRowKeyDisplay(financialData.liabilityData)
                this.setRowKeyDisplay(financialData.profitLossData)
                this.setRowKeyDisplay(financialData.fmvData.stock_price_data)
                this.setRowKeyDisplay(financialData.fmvData.fmv_valuation_and_interest_by_quarter)
                if (refreshedByAuto) {
                    // if the function is triggered by auto save, only update pficTest data
                    this.setState({
                        pficTest: pficTest
                    })
                } else {
                    this.setState({
                        id: result.id,
                        currencyId: result.currencyId,
                        financialData: calculateTotals(financialData),
                        financialData_LastYear: financialData_LastYear,
                        pficTest: pficTest,
                        currencyList: currencyList,
                        incorporationdate: result.incorporationdate,
                        workflowState: newWorkflowState,
                        loading: false,
                        haveData: true,
                        haveData_LastYear: haveData_LastYear,
                        approveButtonEnabled: canInternalApproverEdit(newWorkflowState) && !isInternalApproved(newWorkflowState) && (!isModifiedOrUploadByExternal(newWorkflowState) || (isModifiedOrUploadByExternal(newWorkflowState) && (isExternalApproved(newWorkflowState) || isExternalApprovedOnlyFiles(newWorkflowState) || isExternalAgreed(newWorkflowState)))),
                        financialDataAvailable: result.financialDataAvailable
                    })
                    this.switchFilterType(this.state.balanceSheetInputType, this.state.appendCategory, (isDataConsideredAsFinalized(this.state.workflowState) || this.state.balanceSheetInputType === "append"))
                }
            } else {
                this.setState({
                    loading: false,
                    haveData: false
                })
            }
        }
    ).catch((error) => {
        PopErrors(error.response.data)
        this.setState({
            loading: false,
            haveData: false
        })
    });
}