  uploadFile = (opt) => {
        var name: string = opt.file.name
        if (!(name.endsWith(".txt") || name.endsWith(".doc") || name.endsWith(".docx") || name.endsWith(".csv") || name.toLowerCase().endsWith(".pdf") || name.endsWith(".xls") || name.endsWith(".xlsx")|| name.endsWith(".xlsm"))) {
            deletionConfirmModal({
                title: "Error Occured",
                content: <div>
                    <strong>{name}</strong>
                    <div>File Type Not Supported</div>
                </div>,
                okText: "OK",
                hasDelPic: true,
                onCancel: this.destroyAll,
                cancelText: "OK for all"
            })
        } else {
            var data = new FormData()
            data.append("file", opt.file)
            var currentPromise = this.props.uploadReportFile(this.props.id, data).then((res)=>{
              var response = res.toString()
              if(response.length > 15){
                var warningMsgList = response.split(",")
                var warningMsg = warningMsgList[1]
                var fundName = warningMsgList[2]
                var periodStart = warningMsgList[3]
                var periodEnd = warningMsgList[4]
                if(warningMsg == "Report Date Mismatch"){
                  deletionConfirmModal({
                    title: "Report Date Mismatch",
                    content: <div>
                      <div>The audit period for fund {fundName} ranges from {periodStart} to {periodEnd}, as defined in the fund profile.</div>
                      <div>The following input reports don’t match those dates:</div>
                      <span>{name}</span>
                      <div>Please acknowledge that you are responsible to upload the correct input.</div>
                    </div>,
                    okText: "OK",
                    hasDelPic: false,
                    onCancel: this.destroyAll,
                    cancelText: "OK for all"
                  })
                }else if(warningMsg.length !=0){
                    deletionConfirmModal({
                        title: "Multiple-tab Data Extracted",
                        content: <div>
                          <div><b>{opt.file.name}</b> contains data for both {warningMsg} according to audit period <b>{periodStart}</b> to <b>{periodEnd}</b>.</div>
                          <div>Please acknowledge that you are responsible to upload the correct input.</div>
                        </div>,
                        okText: "OK",
                        hasDelPic: false,
                        onCancel: this.destroyAll,
                        cancelText: "OK for all"
                      })
                }
              }

            }).catch(error => {
              var errorStr = error.toString()
              if(errorStr === "Please upload Dividends Detail Report in income summary format."){
                deletionConfirmModal({
                  title: "Unexpected Report Format",
                  content: <div>
                    <div>The following file does not contain a correctly formatted Dividends Detail Report:</div>
                    <span>{name}</span>
                    <div>Please double-check your input and upload a Dividends Detail Report in the income summary-format.</div>
                  </div>,
                  okText: "OK",
                  hasDelPic: false,
                  onCancel: this.destroyAll,
                  cancelText: "OK for all"
                })
              }else if(errorStr === "File contains 0 records, please check your file and upload again."){
                deletionConfirmModal({
                  title: "Unexpected Report Format",
                  content: <div>
                    <div>{name} contains 0 records, please check your file and upload again.</div>
                  </div>,
                  okText: "OK",
                  hasDelPic: false,
                  onCancel: this.destroyAll,
                  cancelText: "OK for all"
                })
              } else if (errorStr === "there is no report header in this page"){
                deletionConfirmModal({
                  title: "Report Formatting Error",
                  content: <div>
                    <div>This IMS-type report contains text in the header row. This is an error created on the HSBC side. Please contact the fund admin and request a report in proper format.</div>
                    <div><a onClick={this.downloadAccountMovementDocx}>sampleAccountsMovementReport.docx</a></div>
                    <div><a onClick={this.downloadAccountMovementTxt}>sampleAccountsMovementReport.txt</a></div>
                  </div>,
                  okText: "OK",
                  hasDelPic: false,
                  onCancel: this.destroyAll,
                  cancelText: "OK for all"
                })
              }else{
                deletionConfirmModal({
                  title: "Error Occured",
                  content: <div>
                    <strong>{name}</strong>
                    <div>{error}</div>
                  </div>,
                  okText: "OK",
                  hasDelPic: true,
                  onCancel: this.destroyAll,
                  cancelText: "OK for all"
                })
              }
            })

            if (this.uploadPromise === null) {
                this.uploadPromise = currentPromise
                this.setState({ uploading: true })
            } else {
                var wrappedPromise = this.uploadPromise.then(() => currentPromise)
                this.uploadPromise = wrappedPromise
            }
            var finalPromise = this.uploadPromise
            this.uploadPromise.then(() => {

                if (this.uploadPromise === finalPromise) {
                    this.uploadPromise = null
                    this.getReportUploadState()
                }
            })
        }
    }
