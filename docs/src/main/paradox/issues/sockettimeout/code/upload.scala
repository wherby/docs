 def uploadAndGet2: Action[AnyContent] = deadbolt.Pattern(value = "(v_ocr)|(b_ocr)|(v_bs)|(b_bs)", patternType = PatternType.REGEX)(parse.anyContent) { implicit request =>
    val currentUser = CurrentUser(request.subject.get.identifier)
    var resultType:Option[String]=None
    request.body.asMultipartFormData match {
      case Some(mfd) =>
        resultType = mfd.dataParts.get("resultType").flatMap(_.headOption)
      case _ => {
        request.body.asFormUrlEncoded match {
          case Some(data) => {
            resultType = data.get("resultType").flatMap(_.headOption)
          }
          case _ => BadRequest
        }
      }
    }
    upload(request).map(uploadRes => {
      if (uploadRes.header.status == OK) {
        var res = ""
        val materializer = Materializer.matFromSystem(actorSystem)
        val body: String = Await.result(uploadRes.body.consumeData(materializer), Duration.Inf).utf8String
        val resTemplateJson = Json.parse(body)
        val id = (resTemplateJson \ "id").as[String]
        while (res != VendorServiceDao.Status.PROCESSED && res != VendorServiceDao.Status.PROCESS_FAILED) {
          Thread.sleep(1000)
          res = Await.result(getVendorTaskStatus(id)(currentUser).map(maybeS => {
            maybeS.map(s => s.status)
          }), 5.seconds).getOrElse("")
        }
        Await.result(getRes(id, resultType, currentUser), 100.seconds)
      }else if(uploadRes.header.status == BAD_REQUEST){
        uploadRes
      } else {
        InternalServerError("Failed to create task")
      }
    })
  }