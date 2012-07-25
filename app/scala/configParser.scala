package com.huskycode.disco.parser

import com.huskycode.disco.types.Dep
import com.huskycode.disco.reader._

class ConfigParserImpl {
 val playConfig = play.api.Play.current.configuration 

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