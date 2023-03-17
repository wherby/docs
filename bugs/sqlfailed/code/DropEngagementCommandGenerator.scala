class DropEngagementCommandGenerator @Inject() (ServerEnvironment:String) extends CommandGeneratorByServerEnvironment {
  val profile: JdbcProfile = _root_.slick.jdbc.MySQLProfile
  import profile.api._
  def generate(): SqlAction[Int, NoStream, Effect] = {
    sqlu"""
          DROP TABLE IF EXISTS `V_Engagement_#${ServerEnvironment}`;
          """
  }
}