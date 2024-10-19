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

acc = 0
if __name__ == "__main__":
    
    ls = ["8","81","82","829","8299"]
    ls = ls*100
    #ls.append("82888"*10000)
    ls = [a *1000 for a in ls]
    #print(ls)
    sortArr(ls)
    print(acc)
    
