# How to structure a extractor process.

## Using OO design

Caller method

: @@snip[caller mether](./code/geneva/caller.scala)


Process design using OO

PositionAppraisalReportProcessor
: @@snip[PositionAppraisalReportProcessor](./code/geneva/PositionAppraisalReportProcessor.scala)

BaseExcelReportProcessor
: @@snip[BaseExcelReportProcessor](./code/geneva/BaseExcelReportProcessor.scala)

BaseReportProcessor
: @@snip[BaseReportProcessor](./code/geneva/BaseReportProcessor.scala)

AbstractProcessor
: @@snip[AbstractProcessor](./code/geneva/AbstractProcessor.scala)

Configuration file

PositionAppraisalReportConfig
: @@snip[PositionAppraisalReportConfig](./code/geneva/PositionAppraisalReportConfig.scala)

ExtractorConfig
: @@snip[ExtractorConfig](./code/geneva/ExtractorConfig.scala)

## Another way of design

Caller method
: @@snip[Caller method](./code/morgan/caller.scala)


MorganPSRProcessor
: @@snip[MorganPSRProcessor](./code/morgan/MorganPSRProcessor.scala)

MorganExcelPSRReader
: @@snip[MorganExcelPSRReader](./code/morgan/MorganExcelPSRReader.scala)


## How to compare the two ways

The two ways achieves same result.

The OO way use a big configuration to control all behaviors. The functions in
configuration are:

    1. convert excel to standard input 
    2. located the target field in input
    3. extract target input into result
    4. format the result value 

The another way combines the configuration of different functions.