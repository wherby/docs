import java.io.File

import scala.concurrent.Future

def uploadFSLIMapping(fundEngagementId: String) =
  deadbolt.SubjectPresent()(parse.multipartFormData) { implicit request =>
  {
    request.body
      .file("file")
      .map(file => {
        val userEmail      = request.session.get("email").getOrElse("")
        val uploadFileName = file.filename
        val sourceFile     = file.ref.toFile

        val targetFile = new File(
          s"${sourceFile.getPath}.${file.filename.split('.').lastOption.getOrElse("xlsx")}"
        )
        sourceFile.renameTo(targetFile)
        val workbook            = WorkbookFactory.create(new File(targetFile.getAbsolutePath))
        val (result, frontData) = FSLIMappingUtils.getFSLIMappingFromWorkbook(workbook)
        if (result) {
          val futureUpdates = getValidFSLIMappingFromFrontData(fundEngagementId, frontData)
          futureUpdates.flatMap(maybeUpdates => {
            maybeUpdates match {
              case Some(updateContent) => {
                updateFSLIMappingToTableAndFundEngagement(updateContent._1, userEmail, updateContent._2,updateContent._5)
                  .map(res => {
                    Ok(
                      updateContent._3 + " records updated, " + updateContent._4 + " invalid records found."
                    )
                  })
              }
              case None => {
                Future(BadRequest("No valid records found"))
              }
            }
          })
        } else {
          Future(BadRequest("Invalid FSLI mapping file format"))
        }
      })
      .getOrElse(Future(BadRequest("Unknown Error")))
  }
  }