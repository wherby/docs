  priceGroupColumn: EditableColumnProps<SecurityPriceInfo> = {
    title: <div style={{display: "flex"}}>
      <p>Audit Period End </p>
      <p style={{color: "#e03220", fontWeight: "bold"}}>&nbsp;(Price as of {new Date(Number(this.props.auditPeriodEnd.split("-")[0]), Number(this.props.auditPeriodEnd.split("-")[1])-1, Number(this.props.auditPeriodEnd.split("-")[2])).toString().split(" ")[1] + " "
      ...
    </div>,
    ...

 render() {
    let columns = []
    columns=[...columns, this.statusColumn, this.securityCategoryColumn, this.securityNameFundAdminColumn, this.ISINColumn, this.currencyColumn, this.exchangeColumn, this.priceGroupColumn]
    return (
      <div>
        ...
        <div id="securityEditableTable" className="security-price-editable-table">
          <EditableTable
            columns={columns}
            dataSource={this.state.data}
            //dataSource={fakeData}
            controlData={true}
            loading={this.state.loading}
            onModify={this.onModify}
            enableAdd={false}
            pagination={false}
            rowKey="id"
            showControls={false}
          />
        </div>
        ..
      </div>
    )
  }