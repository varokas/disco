package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  def index = Action {
    val r = new com.huskycode.disco.runner.CommandRunner("ls")
    Ok(views.html.index(
      r.runCommand().toString
    ))
  }
  
}
