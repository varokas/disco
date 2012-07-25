package com.huskycode.disco.executor
import com.huskycode.disco.reader._
import com.huskycode.disco.deps._
import com.huskycode.disco.graphdb._
import com.huskycode.disco.types.Dep

class Executor {
  val playConfig = play.api.Play.current.configuration 
  
  def execute() = { 
    val readers = parseReaders()
    GraphDBService.cleanupGraphDb()

    val entitySpecsByReader = readers.map { r => (r, r.getEntitySpecs()) }.toMap

    entitySpecsByReader.foreach { case(r,specs) => 
      specs.foreach { es => GraphDBService.createEntity(
         es.name, es.entityType, r.getName(), r.getContent(es.entityType, es.name)) } 
    }
    
    val readersMap = readers.map{ r => (r.getName(), r) }.toMap
    val deps = parseDeps()

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

  def parseDeps():List[Dep] = {
 
    playConfig.getConfig("deps") match {
      case Some(deps) => {
        val depLines = deps.subKeys.map { deps.getConfig(_).get } 
        depLines.toList.flatMap{ depLine => 
          val from = depLine.getString("from").get
          val tos = depLine.getString("to").get.split(",").toList
          
          return tos.map{ t => new Dep(from, t) }
        }
      }
      case _ => return List() 
    }
  }

  def parseReaders():Iterable[EntityReader] = {
    val readersNode = playConfig.getConfig("readers")
    
    readersNode match {
      case None => return List()
      case Some(readers) => {
        val readerNames = readers.subKeys
        val eachReader = readerNames.map{ r => (r, readers.getConfig(r).get) }
        return eachReader.map{ r => createReader( r._1, r._2 ) }
      }
    } 
  }

  private def createReader(name:String, config: play.api.Configuration):EntityReader = {
    val typeNode = config.getString("type") 
    typeNode match {
      case None => throw new IllegalArgumentException("Cannot find type on reader")
      case Some(t) => {
        t match { 
          case "mssql" => return new MSSQLEntityReader(name,
            config.getString("driver").get,
            config.getString("url").get,
            config.getString("username").get, 
            config.getString("password").get
          )
          case "file" => return new FileEntityReader(name, config.getString("path").get) 
          case _ => throw new IllegalArgumentException("Unrecognized readers type: " + t) 
        }
      }
    }
  }
}
