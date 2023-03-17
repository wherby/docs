fetchData = () => {
    this.props.getPureAdminUsers().then((res) => {
     ....
            for(var i = 0; i < data.length; i++){

              downloaders += parseInt(JSON.parse(data[i].content).statisticsSeq[2].number)     [1]
            }


        ...



  fetchData = () => {
    if(this.state.selectDateType[0] > 0) {
        ...
          for(var i = 0; i < data.length; i++){
            uploadedReports += parseInt(JSON.parse(data[i].content).statisticsSeq[4].number)    [2]
        ...
          }

