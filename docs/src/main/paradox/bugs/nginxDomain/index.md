# nginx config for domain

## Upload failure

In novus project, the frontend could call get method to backend server,
but for post api call the quest always failed.

![POST API FAILED](pic/postapifail.png)

For the nginx setting as below:

![Nginx setting](pic/nginxsetting.png)

From frontend, the POST quest is use the api request:

![API request](pic/apiRequest.png)

So the server name in nginx should use domain name.

After that, we could see the post body can be cached in nginx server:

![POST cached](pic/postCached.png)