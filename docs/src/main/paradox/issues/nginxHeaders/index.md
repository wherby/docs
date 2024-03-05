# Nginx handle request with underscore headers


## Issue

When send request with header "service_user" to domain server with nginx, then the request return 401, when send the same request to ip/server name, the request will success.

For the domain api is proxied by nginx, nginx has a [default behavior](https://stackoverflow.com/questions/26938604/get-headers-with-an-underscore-on-nginx) to remove headers with underscore.
The request headers with underscore will be removed, then the result will be 401