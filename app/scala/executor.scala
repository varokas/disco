package com.huskycode.disco.executor
import com.huskycode.disco.reader._
import com.huskycode.disco.deps._
import com.huskycode.disco.graphdb._
class Executor {
  val playConfig = play.api.Play.current.configuration 
  
  def execute() = { 
    val readers = parseReaders()
    GraphDBService.cleanupGraphDb()
    val entitySpecs = readers.flatMap{ r => r.getEntitySpecs() } 
    entitySpecs.foreach { es => GraphDBService.createEntity(es.name, es.entityType) }     
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
        val eachReader = readers.subKeys.map{ readers.getConfig(_).get }
        return eachReader.map{ createReader( _ ) }
      }
    } 
  }

  private def createReader(config: play.api.Configuration):EntityReader = {
    val typeNode = config.getString("type") 
    typeNode match {
      case None => throw new IllegalArgumentException("Cannot find type on reader")
      case Some(t) => {
        t match { 
          case "mssql" => return new MSSQLEntityReader(
            config.getString("driver").get,
            config.getString("url").get,
            config.getString("username").get, 
            config.getString("password").get
          )
          case "file" => return new FileEntityReader(config.getString("path").get) 
          case _ => throw new IllegalArgumentException("Unrecognized readers type: " + t) 
        }
      }
    }
  }
  
  //private def createDep(config: play.api.Configuration):
}
