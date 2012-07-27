package com.huskycode.disco.reader

import com.huskycode.disco.types._
import java.sql.{ResultSet, DriverManager}

case class EntitySpec(name:String)

trait EntityReader {
   def getName():String
   def getEntitySpecs():Iterable[EntitySpec]
   def getContent(name:String):String
}

abstract class MSSQLEntityReader(name:String, driver:String, url:String, username:String, password:String) extends EntityReader {
	val dbAccess = new DBAccess(driver, url, username, password) 

	protected def getQuery():String
        
    val contentQueryTemplate = "SELECT definition FROM sys.sql_modules where object_id = OBJECT_ID('?')"
    
    override def getName() = name

	override def getEntitySpecs():Iterable[EntitySpec] = {
		dbAccess.query( getQuery(), rs => new EntitySpec(rs.getString("name") ) )
	}

    override def getContent(name:String):String = {
       val contentQuery = contentQueryTemplate.replace("?",name.replace("'","''")) 
       return dbAccess.query(contentQuery, rs => rs.getString("definition")).fold("")((acc,n) => acc + "\n" + n)
    }
}

class MSSQLSPReader(name:String, driver:String, url:String, username:String, password:String) 
  extends MSSQLEntityReader(name,driver,url, username,password) {
	override protected def getQuery() = "SELECT name from sys.procedures"
}

class MSSQLViewReader(name:String, driver:String, url:String, username:String, password:String)
  extends MSSQLEntityReader(name,driver,url, username,password) {
	override protected def getQuery() = "SELECT name from sys.views"
}

class MSSQLTableReader(name:String, driver:String, url:String, username:String, password:String)
  extends MSSQLEntityReader(name,driver,url, username,password) {
	override protected def getQuery() = "SELECT name from sys.tables"
}

class MSSQLTriggerReader(name:String, driver:String, url:String, username:String, password:String)
  extends MSSQLEntityReader(name,driver,url, username,password) {
	override protected def getQuery() = "SELECT name from sys.triggers"
}

class MSSQLFunctionReader(name:String, driver:String, url:String, username:String, password:String)
  extends MSSQLEntityReader(name,driver,url, username,password) {
	override protected def getQuery() = "select name from sys.objects where type='FN'"
}

class FileEntityReader(name:String, filePath:String) extends EntityReader {
	import java.io.File

	val pp = new jregex.util.io.PathPattern(filePath)
	val e = pp.enumerateFiles()
        
        override def getName() = name

	override def getEntitySpecs():Iterable[EntitySpec] = {
	   val l = new scala.collection.mutable.ListBuffer[File]()
	   while(e.hasMoreElements()) {
	     val f = e.nextElement().asInstanceOf[File]
	     if(f.isFile()) {
	     	l += f
	     }
	   }
	   val specs = l.map( f => new EntitySpec(f.getAbsolutePath()) )
	   return specs.toList 
	}

        override def getContent(name:String):String = {
           try {
           	  return scala.io.Source.fromFile(name, "UTF-8").mkString
           }
           catch {
           	  case e: Exception => e.toString()
           }
        }
}

class DBAccess(className: String, uri: String, username:String, password:String) {
  Class.forName(className)
  val conn = DriverManager.getConnection(uri, username, password)

  def query[E](sql:String, mapper:ResultSet => E):List[E] = {
    val stmt = conn.createStatement()
    val rs:ResultSet = stmt.executeQuery(sql)

    Stream.continually(rs).takeWhile(rs => rs.next()).map(mapper).toList
  }
}
