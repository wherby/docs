package com.pwc.ds.awm.processor

import java.sql.Date
import java.time.LocalDate

import com.pwc.ds.awm.processor.OutputMode.Type
import com.pwc.ds.awm.processor.reportconfigs.extractorschema.ExtractorConfig

object OutputMode extends Enumeration {
  type Type = Value
  val Excel = Value("Excel")
  val Memory = Value("Memory")
}

trait AbstractProcessor {
  def readFileAndExtract(sourceFilePath: String, destFilePath: String, mode: Type, extractorConfig: ExtractorConfig, fundName:String, year:Integer): Unit

  def readFileAndExtractToObjectForPeriod(sourceFilePath: String, extractorConfig: ExtractorConfig, fundName: String, frontEndPeriodStart: Date, frontEndPeriodEnd: Date):Any
}
