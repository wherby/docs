# UI crash

## Issue

When user is assigned to specified engagement, then user can't login to the system

![engagement](pic/engagementname.png)

![Cantlogin](pic/cantlogin.png)

The fund name is "Bosheng China Special Value – Fund 2021"

But "–" is different from "-"

![ascii](pic/ascii.png)

The code for the issue is 

```javascript
    try{
        var userResponse = await axios.get(`/user`, {headers:userInfo})
        if(userResponse.data){
          let privilege = userResponse.data ? userResponse.data.userInfoFront.privilege : ""
          //window.sessionStorage.setItem("userInfo",btoa(JSON.stringify(userResponse.data)))
          store.dispatch(setPrivilege(privilege))
          var loginValue = this.email || "my account";
          this.props.login(loginValue);
          let state = this.props.location.state || {};
          window.location.href = state.from || '/';
        }
        else {
          this.setState({
            ishiddenrerror: false
          });
        }
      }
      catch (e) {
        this.setState({
          ishiddenrerror: false
        })
      }
```

The btoa will throw exception,while catch will not print anything about this exception.