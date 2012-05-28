package com.huskycode.disco.executor
import com.huskycode.disco.reader._
import com.huskycode.disco.deps._
import com.huskycode.disco.graphdb._
class Executor {
  val playConfig = play.api.Play.current.configuration 
  
  def execute() = { 
    val readers = parseReaders()
    GraphDBService.cleanupGraphDb()
    readers.foreach { r => 
      val entitiesSpec = r.getEntitySpecs()
      entitiesSpec.foreach { es => GraphDBService.createEntity(es.name, es.entityType, r.getName()) } 
    }
  }

  def parseDeps():DepsBuilder = {
    val depsBuilder = new DepsBuilder() 
    playConfig.getConfig("deps") match {
      case Some(deps) => {
        val depLines = deps.subKeys.map { deps.getConfig(_).get } 
        depLines.foreach{ depLine => 
          val from = depLine.getString("from").get
          val tos = depLine.getString("to").get.split(",").toList
          depsBuilder.addNode(from,tos)
        }
      } 
    }
    return depsBuilder
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
  
  //private def createDep(config: play.api.Configuration):
}
