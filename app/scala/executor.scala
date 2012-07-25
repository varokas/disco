package com.huskycode.disco.executor
import com.huskycode.disco.deps._
import com.huskycode.disco.graphdb._
import com.huskycode.disco.parser._

class Executor {
  val configParser = new ConfigParserImpl()

  
  def execute() = { 
    val readers = configParser.parseReaders()
    GraphDBService.cleanupGraphDb()

    val entitySpecsByReader = readers.map { r => (r, r.getEntitySpecs()) }.toMap

    entitySpecsByReader.foreach { case(r,specs) => 
      specs.foreach { es => GraphDBService.createEntity(
         es.name, es.entityType, r.getName(), r.getContent(es.entityType, es.name)) } 
    }
    
    val readersMap = readers.map{ r => (r.getName(), r) }.toMap
    val deps = configParser.parseDeps()

    for(dep <- deps) {
      val fromReader = readersMap.get(dep.from).get
      val toReader = readersMap.get(dep.to).get

      val fromEntities = entitySpecsByReader.get(fromReader).get
      val toEntities = entitySpecsByReader.get(toReader).get

      for(fromEntitySpec <- fromEntities) {
        for(toEntitySpec <- toEntities) {
          val fromEntity = GraphDBService.getEntity(fromEntitySpec.name, fromEntitySpec.entityType)
          if(containsEntity(fromEntity.content, toEntitySpec.name)) {
             val toEntity = GraphDBService.getEntity(toEntitySpec.name, toEntitySpec.entityType)

             GraphDBService.createDependency(fromEntity, toEntity)
          }
        }
      }
    }

    
  }

  def containsEntity(content:String, entityName:String) = if(content != null) content.contains(entityName) else false
}
