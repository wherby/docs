 lazy val secureSource = config.getOptional[Boolean]("useidam") match {
    case Some(true) => config.getOptional[Boolean]("pwcoauth.useoauth") match {
      case Some(true) => index3
      case _ => index2
    }
    case _ => index
  }

  def index2: Action[AnyContent] = {
    secureAction(
      assets.at("index.html")
    )
  }

  def index3: Action[AnyContent] = {
    secureActionOauth(
      assets.at("index.html")
    )
  }

  def index: Action[AnyContent] = assets.at("index.html")
