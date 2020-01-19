
class Module(environment: Environment,
             configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
      ...
    bind(classOf[UserDAO]).to(classOf[SlickUserDAO])
    bind(classOf[UserRead]).to(classOf[UserReadImpl])
    bind(classOf[UserWrite]).to(classOf[UserWriteImpl])
      ...
  }
}

