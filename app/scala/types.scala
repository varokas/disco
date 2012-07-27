package com.huskycode.disco.types

trait Entity {
	def name: String
    def reader: String
    def content: String
}

object EntityType extends Enumeration {
	type EntityType = Value
 	val StoredProc, View, Table, Trigger, File, Function = Value

    def getDBTypes() = List(StoredProc, View, Table, Trigger, Function)
}

case class Dep(from:String, to:String)
