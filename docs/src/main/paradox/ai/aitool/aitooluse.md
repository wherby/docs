

# Tool

There is an AI stock tool which is developed by starriv:
[worth-buy-stocks](https://github.com/alpacahq/alpaca-skills)

The tool is using stock api published by Alpaca Markets:
[Alpaca Markets](https://github.com/alpacahq/alpaca-skills)


When you want to use the tool is very simple:
![use](./pic/1.png)
![use](./pic/2.png)
![use](./pic/3.png)

and one question will cost:
![cost](./pic/4.png)


Alpaca Markets is very complex api tool , use the skill which will help us to use the tool in zero knowledge.

However, if the workflow is properly configured, we might be able to skip the AI function altogether.

When I switched the AI model to DeepSeek-Flash and ran the same command, a different workflow was triggered, as shown below:


![DS-Flash](./pic/5.png)
![DS-Flash](./pic/6.png)
The workflow then launched over 100 agents, forcing me to terminate it manually:
![DS-Flash](./pic/7.png)

![DS-Flash](./pic/8.png)
![DS-Flash](./pic/9.png)

This run ended up costing 2.42 (see below):
![DS-Flash](./pic/10.png)

After that, I asked it to use the correct model:
![DS-Flash](./pic/11.png)
![DS-Flash](./pic/12.png)

This time, the cost was only 1 cent:
![DS-Flash](./pic/13.png)

So it appears that the AI may have selected an incorrect workflow, resulting in a cost that was roughly 200 times higher than necessary.