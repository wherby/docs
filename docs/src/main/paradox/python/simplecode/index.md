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

```python
from functools import cmp_to_key
def compare(a,b):
    global acc
    acc +=(len(a) + len(b)) *2
    if (a+b)==(b+a):return 0
    return 1 if  (a+b)>(b+a) else -1

def sortArr(ls):
    ls = sorted(ls,key =cmp_to_key(compare),reverse=True)
    #print(ls)
    return "".join(ls)

# https://stackoverflow.com/questions/18262306/quicksort-with-python
def qsort(array):
    """Sort the array by using quicksort."""

    less = []
    equal = []
    greater = []

    if len(array) > 1:
        pivot = array[0]
        for x in array:
            r = compare(x,pivot)
            if r<0:
                less.append(x)
            elif r==0:
                equal.append(x)
            elif r>0:
                greater.append(x)
        # Don't forget to return something!
        return qsort(less)+equal+qsort(greater)  # Just use the + operator to join lists
    # Note that you want equal ^^^^^ not pivot
    else:  # You need to handle the part at the end of the recursion - when you only have one element in your array, just return the array.
        return array

def sortArr2(ls):
    ls = qsort(ls)
    #print(ls)
    return "".join(ls[::-1])

acc = 0
if __name__ == "__main__":
    
    ls = ["8","81","82","829","8299"]
    ls = ls*1000
    #ls.append("82888"*10000)
    ls = [a *1000 for a in ls]
    #print(ls)
    a = sortArr(ls)
    print(acc)
    acc =0
    b = sortArr2(ls)
    print(acc)
    print(a==b)
    print(a[:100],b[:100])
    print(a[-100:],b[-100:])
```

result is

```
293156000
144000000
True
8888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888 8888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888888
8181818181818181818181818181818181818181818181818181818181818181818181818181818181818181818181818181 8181818181818181818181818181818181818181818181818181818181818181818181818181818181818181818181818181
```