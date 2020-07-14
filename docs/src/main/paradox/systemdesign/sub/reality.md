## What's reality of software development?

### [Big ball of mud](https://corecursive.com/22-big-ball-of-mud-architecture-and-services-with-wade-waldron/)

![Big ball](../pic/mudball.png)

![shanty town](../pic/shantytown.png)  ![spacestation](../pic/spacestation.png)

Code for authentication:

For application authentication, there are 3 types: by application, by SAML at first stage,
and add by Oauth in later version. 
For authorization, there is not needed any change for different change.

Code for frontend controller:
: @@snip[Code for frontend controller](../code/authentication.scala)

Saml
: @@snip[Saml](../code/saml.scala)

Oauth
: @@snip[Oauth](../code/oauth.scala)

Authentication
: @@snip[Authentication](../code/authorization.scala)