package controllers

import com.digitaltangible.playguard.{IpRateLimitFilter, RateLimiter}
import javax.inject._
import play.api._
import play.api.mvc._

import scala.concurrent.Future

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  private val ipRateLimitFilter = IpRateLimitFilter[Request](
    new RateLimiter(3, 1f / 60, "test limit by IP address"), { r: RequestHeader =>
      Future.successful(TooManyRequests(s"""rate limit for ${r.remoteAddress} exceeded"""))
    }
  )

  lazy val rateAction = Action andThen ipRateLimitFilter
  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = rateAction { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }
  
  def explore() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.explore())
  }
  
  def tutorial() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.tutorial())
  }
  
}
