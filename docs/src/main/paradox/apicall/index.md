# Api call failed

## Issue

In the workflow, the account number page is laoding:

![Loading for](pic/loading.png)

It's caused by api error :

![API call failed](pic/apicall.png)

## Code

```javascript
 fetchData = () => {
    this.setState({
      loading: true
    })
   ..
    this.props.getFundEngagementById(this.props.id).then((fund) => {
      ..
      this.props.getAccountNameList(this.state.fundEngagementId).then((data)=>{
       ...
          this.setState({data: dataList, loading: false, accountNumMapFilledList: accountNumMapFilledList, accountNumMapUnfilledList: accountNumMapUnfilledList})
          ...
```


## How to fix

1. The robustness of API call
2. How to wrap the state change for API call