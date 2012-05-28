package com.huskycode.disco.reader

import com.huskycode.disco.types._
import java.sql.{ResultSet, DriverManager}

case class EntitySpec(name:String, entityType:EntityType.Value)

trait EntityReader {
   def getName():String
	def getEntitySpecs():Iterable[EntitySpec]
}

class MSSQLEntityReader(name:String, driver:String, url:String, username:String, password:String) extends EntityReader {
	val queries = Map( 
		EntityType.StoredProc -> "SELECT name from sys.procedures",
		EntityType.View -> "SELECT name from sys.views",
		EntityType.Table -> "SELECT name from sys.tables",
		EntityType.Trigger -> "SELECT name from sys.triggers",
		EntityType.Function -> "select name from sys.objects where type='FN'"
	)
        
        override def getName() = name

	override def getEntitySpecs():Iterable[EntitySpec] = {
	        val dbAccess = new DBAccess(driver, url, username, password) 
		queries.flatMap{ case (typ, query) => dbAccess.query( query, rs => new EntitySpec(rs.getString("name"), typ)  )}
	}
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

//"SELECT definition FROM sys.sql_modules where object_id = OBJECT_ID(?)"
//"SELECT " + columnName + " FROM "+ tableName, String.class)
