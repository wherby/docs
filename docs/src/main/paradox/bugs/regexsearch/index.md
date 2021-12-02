# Regex failed in frontend

## Issue

Search without bracket success :

![Success search](pic/searchSucc.png)

Search failed with bracket:

![Search failed](pic/searchFail.png)

When search with half bracket, the UI will crash:

![Search error](pic/error.png)

The reason for the failed is using regex in frontend, when in  backend the search 
will success.

![Success 2 ](pic/searchSuc2.png)

When search in backend, Slick can handle the special letter; while in frontend, 
the input should be verified. Don't trust user's input.