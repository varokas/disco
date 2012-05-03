package com.huskycode.disco.reader

import com.huskycode.disco.types._

case class EntitySpec(name:String, entityType:EntityType.Value)

trait EntityReader {
	def getEntity():Iterable[EntitySpec]
}

class MSSQLEntityReader extends EntityReader {
	override def getEntity():Iterable[EntitySpec] = null

	private def getTables():Iterable[EntitySpec] = null
	private def getViews():Iterable[EntitySpec] = null
	private def getStoreProcs():Iterable[EntitySpec] = null
	private def getTriggers():Iterable[EntitySpec] = null
}