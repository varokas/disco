package controllers

import play.api._
import play.api.mvc._

import com.huskycode.disco.types._
import com.huskycode.disco.graphdb._ 
import jregex._

import org.apache.commons.lang3.StringEscapeUtils

object Application extends Controller {
  def index = Action {
    val allEntities = EntityType.values.flatMap{ t => GraphDBService.getEntities(t) }
    val sortedEntitiesMap = allEntities
          .groupBy{ _.reader }
          .mapValues{ e => e.toList.sortBy(_.name) }
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
    val dependsBy = GraphDBService.getDependByEntites(name, entityTypeEnum)
    Ok(views.html.view(entity,getAnnoatedContent(entity.content, dependsOn), dependsOn, dependsBy))
  }

  private def getAnnoatedContent(content:String, dependsOn:Iterable[Entity]):String = {
    var modifiedContent = StringEscapeUtils.escapeHtml4(content)
    for( dependOnEntity <- dependsOn ) {
      val pattern = new jregex.Pattern("[\\s^]" + dependOnEntity.name + "[\\s$]")
      val replacer = pattern.replacer(new jregex.Substitution() {
        override def appendSubstitution(m: MatchResult, dest:TextBuffer) = {
          val captured = m.group(0)
          dest.append(
            captured.replaceAll(dependOnEntity.name, getLinkToEntity(dependOnEntity))      
          ) 
        }
      })

      modifiedContent = replacer.replace(modifiedContent)
    } 
    return modifiedContent
  }

  private def getLinkToEntity(entity: Entity):String = {
    "<a class='ent_" + entity.name + "' style='background-color:yellow' href='" + 
    controllers.routes.Application.view(entity.entityType.toString,entity.name).toString + 
    "'>" +
    entity.name + 
    "</a>"
  }
}
