package com.huskycode.disco.executor
import com.huskycode.disco.reader._

class Executor {

  def parseReaders():Iterable[EntityReader] = {
    val playConfig = play.api.Play.current.configuration 
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
}
