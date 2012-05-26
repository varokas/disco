package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  def index = Action {
    Ok(views.html.index(
       new com.huskycode.disco.executor.Executor().parseReaders().toString
    ))
  }
  
}
