  componentDidMount(){
    if(this.props.auditPeriodEnd){
      this.fetchData()
    }
  }
componentDidUpdate(previousProps:UploadGSPTableProps){
  if(!previousProps.auditPeriodEnd && this.props.auditPeriodEnd ){
    this.fetchData()
  }
}

## because fetchData only need to be executed once, and only the props are ready, the operation need to be triggered.