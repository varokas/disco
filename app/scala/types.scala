package com.huskycode.disco.types

trait Entity {
	def name: String
	def entityType: EntityType.Value
        def reader: String
}

object EntityType extends Enumeration {
	type EntityType = Value
 	val StoredProc, View, Table, Trigger, File, Function = Value
}

