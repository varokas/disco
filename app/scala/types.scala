package com.huskycode.disco.types

trait Entity {
	def name: String
	def entityType: EntityType.Value
}

object EntityType extends Enumeration {
	type EntityType = Value
 	val StoredProc, View, Table, Trigger, File = Value
}

