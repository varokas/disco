package controllers

import play.api._
import play.api.mvc._

import com.huskycode.disco.types._
import com.huskycode.disco.graphdb._ 
import jregex._

import org.apache.commons.lang3.StringEscapeUtils

object Application extends Controller {
  def index = Action {
    val allEntities = GraphDBService.getEntities()
    val sortedEntitiesMap = allEntities
          .groupBy{ _.reader }
          .mapValues{ e => e.toList.sortBy(_.name) }
    Ok(views.html.list(sortedEntitiesMap))
  }  

  def refresh = Action {
    new com.huskycode.disco.executor.Executor().execute()
    
    Ok("okay")
  }  

  def view(reader:String, name:String) = Action {
    val entity = GraphDBService.getEntity(name,reader) 
    val dependsOn = GraphDBService.getDependOnEntites(name, reader)
    val dependsBy = GraphDBService.getDependByEntites(name, reader)
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
    controllers.routes.Application.view(entity.reader,entity.name).toString + 
    "'>" +
    entity.name + 
    "</a>"
  }
}
