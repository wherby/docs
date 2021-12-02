# Function and Val in code

## Issue

In scala  function and val are quiet equal as below:

def f1 = (a: Int) => { a *2}
val f1 = (a: Int) => { a *2}

In some case the two will have different feature:

When use function, every thing as expected:
![Functiin](pic/def.png)

When use val, the null pointer error show up:
![Val](pic/val.png)


The code as below:

ExtractorConfig
: @@snip[HSBCMFLikeExtractor](code/HSBCMFLikeExtractor.scala)

Reader Instance
: @@snip[SecHSBCMultiTBReader](code/SecHSBCMultiTBReader2.scala)


