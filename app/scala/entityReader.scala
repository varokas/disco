package com.huskycode.disco.reader

import com.huskycode.disco.types._
import java.sql.{ResultSet, DriverManager}

case class EntitySpec(name:String, entityType:EntityType.Value)

trait EntityReader {
   def getName():String
   def getEntitySpecs():Iterable[EntitySpec]
   def getContent(entityType:EntityType.Value, name:String):String
}

abstract class MSSQLEntityReader(name:String, driver:String, url:String, username:String, password:String) extends EntityReader {
	val dbAccess = new DBAccess(driver, url, username, password) 

	protected def getQuery():String
	protected def getEntityType():EntityType.Value
        
    val contentQueryTemplate = "SELECT definition FROM sys.sql_modules where object_id = OBJECT_ID('?')"
    
    override def getName() = name

	override def getEntitySpecs():Iterable[EntitySpec] = {
		dbAccess.query( getQuery(), rs => new EntitySpec(rs.getString("name"), getEntityType()) )
	}

    override def getContent(entityType:EntityType.Value, name:String):String = {
       val contentQuery = contentQueryTemplate.replace("?",name.replace("'","''")) 
       return dbAccess.query(contentQuery, rs => rs.getString("definition")).fold("")((acc,n) => acc + "\n" + n)
    }
}

class MSSQLSPReader(name:String, driver:String, url:String, username:String, password:String) 
  extends MSSQLEntityReader(name,driver,url, username,password) {
	override protected def getQuery() = "SELECT name from sys.procedures"
	override protected def getEntityType() = EntityType.StoredProc
}

class MSSQLViewReader(name:String, driver:String, url:String, username:String, password:String)
  extends MSSQLEntityReader(name,driver,url, username,password) {
	override protected def getQuery() = "SELECT name from sys.views"
	override protected def getEntityType() = EntityType.View
}

class MSSQLTableReader(name:String, driver:String, url:String, username:String, password:String)
  extends MSSQLEntityReader(name,driver,url, username,password) {
	override protected def getQuery() = "SELECT name from sys.tables"
	override protected def getEntityType() = EntityType.Table
}

class MSSQLTriggerReader(name:String, driver:String, url:String, username:String, password:String)
  extends MSSQLEntityReader(name,driver,url, username,password) {
	override protected def getQuery() = "SELECT name from sys.triggers"
	override protected def getEntityType() = EntityType.Trigger
}

class MSSQLFunctionReader(name:String, driver:String, url:String, username:String, password:String)
  extends MSSQLEntityReader(name,driver,url, username,password) {
	override protected def getQuery() = "select name from sys.objects where type='FN'"
	override protected def getEntityType() = EntityType.Function
}

class FileEntityReader(name:String, filePath:String) extends EntityReader {
	import java.io.File

	val pp = new jregex.util.io.PathPattern(filePath)
	val e = pp.enumerateFiles()
        
        override def getName() = name

	override def getEntitySpecs():Iterable[EntitySpec] = {
	   val l = new scala.collection.mutable.ListBuffer[File]()
	   while(e.hasMoreElements()) {
	     l += e.nextElement().asInstanceOf[File]
	   }
	   val specs = l.map( f => new EntitySpec(f.getAbsolutePath(), EntityType.File) )
	   return specs.toList 
	}

        override def getContent(entityType:EntityType.Value, name:String):String = {
           return scala.io.Source.fromFile(name).mkString
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
