package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  val mssqlReader = new com.huskycode.disco.reader.MSSQLEntityReader()

  def index = Action {
    Ok(views.html.index(
    	mssqlReader.getEntitySpecs().toString
    ))
  }
  
}