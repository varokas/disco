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
        return eachReader.flatMap{ r => createReader( r._1, r._2 ) }
      }
    } 
  }

  private def createReader(name:String, config: play.api.Configuration):Iterable[EntityReader] = {
    val typeNode = config.getString("type") 
    typeNode match {
      case None => throw new IllegalArgumentException("Cannot find type on reader")
      case Some(t) => {
        t match { 
          case "db" => parseDb(config)
          case "file" => return List(new FileEntityReader(name, config.getString("path").get))
          case _ => throw new IllegalArgumentException("Unrecognized readers type: " + t) 
        }
      }
    }
  }

  private def parseDb(config: play.api.Configuration):List[EntityReader] = {
    val dbConfig = parseDbConfig(config.getConfig("dbConfig").get)
    config.subKeys
        .filter{  key => !(key.equals("dbConfig") || key.equals("type")) }
		.map { key => parseDbReader(key, config.getConfig(key).get, dbConfig)  }.toList
  }

  private def parseDbReader(name: String, config: play.api.Configuration, dbConfig: DBConfig):EntityReader = {
  	config.getString("type") match {
  		case None => throw new IllegalArgumentException("Cannot find type on db reader")
  		case Some(t) => {
	        t match { 
	          case "mssql_stored_proc" => new MSSQLSPReader(name, dbConfig.driver,dbConfig.url, dbConfig.username, dbConfig.password)
	          case "mssql_view" => new MSSQLViewReader(name, dbConfig.driver,dbConfig.url, dbConfig.username, dbConfig.password)
	          case "mssql_table" => new MSSQLTableReader(name, dbConfig.driver,dbConfig.url, dbConfig.username, dbConfig.password)
	          case "mssql_trigger" => new MSSQLTriggerReader(name, dbConfig.driver,dbConfig.url, dbConfig.username, dbConfig.password)
	          case "mssql_function" => new MSSQLFunctionReader(name, dbConfig.driver,dbConfig.url, dbConfig.username, dbConfig.password)
	          case _ => throw new IllegalArgumentException("Unrecognized db readers type: " + t) 
	        }
    	}
  	}
  }

  case class DBConfig(driver:String, url:String, username:String, password:String)

  private def parseDbConfig(config: play.api.Configuration):DBConfig = {
  	new DBConfig(config.getString("driver").get,
                 config.getString("url").get,
                 config.getString("username").get, 
                 config.getString("password").get)
  }
}