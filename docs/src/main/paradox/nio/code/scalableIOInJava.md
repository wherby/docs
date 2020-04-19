## Scalable IO in Java

The original reference is from [Scalable IO in Java](http://gee.cs.oswego.edu/dl/cpjslides/nio.pdf)  { open=new }

#### Classic way of handle one requst pre thread
![Class way](pic/classic.png)

Add design multiple implementations of Event Driven design

#### Single threaded version [Nodejs]
![Single threaded version](pic/single.png)

#### Thread Pool version
![Thread pool](pic/threadPool.png)

#### Multiple reactor
![Using Multiple Reactors](pic/multiReactor.png)