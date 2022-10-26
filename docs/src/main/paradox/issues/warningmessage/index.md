# Suppress insecurerequestwarning

## Issue 

The warning message shows in some processor

![Warning message](pic/warningmessage.png)

The warning message is recorded by file output:
![File output](pic/fileoutput.png)

we could add test code to check which processor generate the error code:

: @@snip[add TestCode](code/addTestCode.py)

## fix the code

According to https://stackoverflow.com/questions/27981545/suppress-insecurerequestwarning-unverified-https-request-is-being-made-in-pytho

code fix:
![Codefix](pic/codefix.png)


![Warning Remove](pic/warningRemove.png)






