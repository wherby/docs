# IE issues for javascript

## Download failed

In AWM there is a function to download table content as excel, but the download function 
failed on IE.

![Download in IE](./pic/download.png)


The code for download as blow:

@@ snip [download javascript](./code/download.js)


The javascript download a blob in frontend. But this operation will be "access deny" as below:

![Access deny](./pic/accessdeny.png)


The MS has its own function(msSaveBlob) to save blob, and IE only support this function [msSaveBlob](https://github.com/mholt/PapaParse/issues/175). 


@@snip [fix download](./code/fixdownload.js)


## Sidebar missing in IE

Sidebar works well in chrome:

![Sidebar in chrome](./pic/frontendchrome.png)

In one page, the sidebar is missing in IE:

![Sidebar in IE](./pic/frontendie.png)

The width in IE is only 0.15px.

To fix this issue is straightforward:

![Fix code](pic/codefix.png)



