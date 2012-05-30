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

  def refresh = Action {
    new com.huskycode.disco.executor.Executor().execute()
    
    val entitiesMap = EntityType.values.map { t => (t, GraphDBService.getEntities(t)) }.toMap 
    val entitiesCount = entitiesMap.mapValues{ e => e.size }
    Ok(views.html.refresh(entitiesCount))
  }  

  def view(entityType:String, name:String) = Action {
    import com.huskycode.disco.executor._

    val entityTypeEnum = EntityType.withName(entityType)

    val readers = new Executor().parseReaders()
    val readersMap = readers.map{ r => (r.getName(), r) }.toMap
    
    val entity = GraphDBService.getEntity(name,entityTypeEnum) 
    val content = readersMap(entity.reader).getContent(entityTypeEnum, name)
    Ok(views.html.view(entity,content))
  }
}
