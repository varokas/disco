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
  }  

  def refresh = Action {
    new com.huskycode.disco.executor.Executor().execute()
    
    val entitiesMap = EntityType.values.map { t => (t, GraphDBService.getEntities(t)) }.toMap 
    val entitiesCount = entitiesMap.mapValues{ e => e.size }
    Ok(views.html.refresh(entitiesCount))
  }  

  def view(entityType:String, name:String) = Action {
    val entityTypeEnum = EntityType.withName(entityType)
    val entity = GraphDBService.getEntity(name,entityTypeEnum) 
    val dependsOn = GraphDBService.getDependOnEntites(name, entityTypeEnum)
    Ok(views.html.view(entity,entity.content, dependsOn))
  }
}
