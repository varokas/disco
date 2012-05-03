package com.huskycode.disco.reader

import com.huskycode.disco.types._
import java.sql.{ResultSet, DriverManager}

case class EntitySpec(name:String, entityType:EntityType.Value)

trait EntityReader {
	def getEntitySpecs():Iterable[EntitySpec]
}

class MSSQLEntityReader extends EntityReader {
	val queries = Map( 
		EntityType.StoredProc -> "SELECT name from sys.procedures",
		EntityType.View -> "SELECT name from sys.views",
		EntityType.Table -> "SELECT name from sys.tables",
		EntityType.Trigger -> "SELECT name from sys.triggers",
		EntityType.Function -> "select name from sys.objects where type='FN'"
	)

	val dbAccess = new DBAccess(
		play.Play.application().configuration().getString("sqlreader.main.driver"),
		play.Play.application().configuration().getString("sqlreader.main.url"),
		play.Play.application().configuration().getString("sqlreader.main.username"),
		play.Play.application().configuration().getString("sqlreader.main.password")
	)

	override def getEntitySpecs():Iterable[EntitySpec] = {
		queries.flatMap{ case (typ, query) => dbAccess.query( query, rs => new EntitySpec(rs.getString("name"), typ)  )}
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