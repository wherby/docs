# UI refreshing issue

## Issue

User report UI is extramly slow:
![User report](pic/issue.png)

When user edit financial data:
![Edit](pic/edit.png)

Then the UI will continuely loading:
![Eror](pic/error.png)

The issue is caused by code:

![Code](pic/code.png)

The function code
: @@snip[The function code](code/getFinancialDataAndSetState.js)


the issue:

``` javascript
if (refreshedByAuto) {
    // if the function is triggered by auto save, only update pficTest data
    this.setState({
        pficTest: pficTest
    })
}
```

How to handle the status code updating in long function?

