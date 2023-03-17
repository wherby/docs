def secureAction[A](action: Action[A]): Action[A] = Secure("SAML2Client").async(action.parser) { implicit request =>
  ...
  val userInfoMap = Map("email"->profiles.head.getAttribute(emailAttrStr).toString.stripPrefix("[").stripSuffix("]"))
  ...
  userTimeOpt match {
    case Some(userTime) if nowTime - userTime.toInt > maxTimeout =>
      Future( Redirect("/").flashing("success" -> "Session timeout, you need to login.")
        .discardingCookies(DiscardingCookie("PLAY_SESSION")).withNewSession.withHeaders("Cache-Control"-> "no-cache"))
    case _=>    action(request).map {
      result =>
        result.withSession(request.session.+("email"->userInfoMap("email"))
          .+("useidm"->"true")
          .+("userTime"->nowTime.toString)
          .+("privilege" -> privilege)).withHeaders("Cache-Control"-> "no-cache")
    }
  }
}
