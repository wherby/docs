# Simple question

## Question

Given an array of strings, return the string formed by concatenating all the strings in the array in the maximum lexicographic order.

```
input：
["8","81","82","829"]
```

return "88298281"

```python
from functools import cmp_to_key
def compare(a,b):
    if (a+b)==(b+a):return 0
    return 1 if  (a+b)>(b+a) else -1

def sortArr(ls):
    ls = sorted(ls,key =cmp_to_key(compare),reverse=True)
    print(ls)
    return "".join(ls)
```
The trap of implement the compare function see [the link](https://stackoverflow.com/questions/79058530/python-sort-lib-different-behavior-with-different-comparing-functions-some-comp)


The following question is "Analyze the algorithm's time complexity and suggest potential optimizations to improve its efficiency".