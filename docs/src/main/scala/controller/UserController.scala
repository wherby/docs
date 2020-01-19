import scala.concurrent.ExecutionContext

@Singleton
class UserController @Inject()(userServiceRead: UserRead,
                               userServiceWrite: UserWrite,
                                ...)
                              (implicit ec: ExecutionContext) extends AbstractController(cc) {
    ...

}
