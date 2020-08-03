def securityPriceFileRecord() = deadbolt.SubjectPresent()(parse.multipartFormData) {
  implicit request => {
    request.body.file("file").map {
      file =>
        val excelResult = ExcelToArray.excelToArray(file.ref.toFile.getAbsolutePath)
        val priceSeqOpt = RecordToSecurityPrice.getInputExcel(excelResult)
        priceSeqOpt match {
          case Some(priceSeq) => Future(priceSeq.map {
            priceTemp => securityPriceWrite.recordSecurityPrice(priceTemp)
          })
            Future(Ok(s"${priceSeqOpt.get.length} records recorded."))
          case None => Future(BadRequest("This does not seem to be a GSP output file."))
        }
    }.getOrElse(Future(BadRequest))
  }
}