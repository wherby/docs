  renderUploadGSP = () => {
    if (this.state.operationType == "uploadGSP") {
      return <UploadGSPOutputList id={this.props.id} engagementId={this.props.engagementId}
      fundName={this.props.fundName} auditPeriodEnd={this.props.auditPeriodEnd} />
    } else return null
  }
  render() {
    return (
      <div className="valuations-container">
        {this.renderUploadGSP()}
      </div>
    )
  }