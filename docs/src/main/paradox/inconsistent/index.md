# Inconsistent status

Statistics query goes different for "downloaders" in User Information section and "Single EGA 
Download" in Fund Information section 

![Statistics Image](./pic/statistics.png)


But the two information comes from same api:

@@ snip [query api](./code/api.scala)

And we could see from UI, the query goes like:

@@ snip [UI query](./code/ui.tsx)


but check the response body of the api:

@@ snip [Response body](./code/responsebody.txt)