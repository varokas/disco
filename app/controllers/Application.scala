package controllers

import play.api._
import play.api.mvc._

import com.huskycode.disco.types._
import com.huskycode.disco.graphdb._ 

object Application extends Controller {
  def index = Action {
    val entitiesMap = EntityType.values.map{ t => (t, GraphDBService.getEntities(t)) }.toMap
    val sortedEntitiesMap = entitiesMap
          .mapValues{ el => el.map(_.name) }  
          .mapValues{ e => e.toList.sorted }
    Ok(views.html.list(sortedEntitiesMap))
    //Ok(views.html.index(
        //play.api.Play.current.configuration.getConfig("deps").get.getConfig("d1").get.getString("to").get.toString
     // Ok(views.html.list())
      //new com.huskycode.disco.executor.Executor().execute().toString
    //))
  }  
  
}
