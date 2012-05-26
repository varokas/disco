package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  def index = Action {
    Ok(views.html.index(
        //play.api.Play.current.configuration.getConfig("deps").get.getConfig("d1").get.getString("to").get.toString
      new com.huskycode.disco.executor.Executor().parseDeps().getLookupMap().toString
    ))
  }  
  
}
