# Fast and slow jobs

## Basic fast jobs flow

Fist think a workflow contains 4 jobs, each job cost 5 minutes to finish
The parallel job number is 4
The second job can only run 2 at same time 

![basic](./pic/basic.png)


## With Slow job

And at same condition, the first 4 job's second job is slow job

![Slow job](./pic/slowjob.png)


## With sperated 

If we split the slow job to sperated job queue

![Split slow](./pic/splitslow.png)

## Split workflow

If we split slow and fast workflow with 2 fast channel and 2 slow channel

![2slow](./pic/2slowjob.png)

With 3 fast channel and 1 slow channel

![1slow](./pic/1slowjob.png)