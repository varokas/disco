package com.huskycode.disco.graphdb

import com.huskycode.disco.types._

import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb._
import org.neo4j.tooling.GlobalGraphOperations

import scala.collection.JavaConversions._

object GraphDBService extends GraphDBService {

}

class GraphDBService(val graphDbDir:String) {
	def this() = this("target/db/graphDb")

	val ENTITY_INDEX_KEY = "entity"
	val TYPE_INDEX_KEY = "type"

	val graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(graphDbDir);
	val index = graphDb.index().forNodes("nodes") 
	registerShutdownHook()

	def createEntity(name:String, entityType:EntityType.Value, reader:String):Entity = {
		var entity:Neo4jEntity = null
		val tx = graphDb.beginTx()
		try
		{
			val node = graphDb.createNode()
			node.setProperty(Neo4jEntity.NAME_PROPERTY, name)
			node.setProperty(Neo4jEntity.TYPE_PROPERTY, entityType.toString)
                        node.setProperty(Neo4jEntity.READER_PROPERTY, reader)

			entity = new Neo4jEntity(node)
			index.add(entity.neo4jNode, ENTITY_INDEX_KEY, getIndexValue(name, entityType))
			index.add(entity.neo4jNode, TYPE_INDEX_KEY, entityType.toString)
			tx.success()
		}
		finally
		{
		    tx.finish()   
		}
		return entity
	}

	def getEntity(name:String, entityType:EntityType.Value):Entity = {
		val result = index.get(ENTITY_INDEX_KEY, getIndexValue(name, entityType)).getSingle
		return if(result != null) new Neo4jEntity(result) else null
	}

	def getEntities(entityType:EntityType.Value):List[Entity] = {
		val result = index.get(TYPE_INDEX_KEY, entityType.toString)
		var lb = new scala.collection.mutable.ListBuffer[Neo4jEntity]()
		while(result.hasNext()) {
			lb += new Neo4jEntity(result.next())
		}

		return lb.toList
	}

	private def getIndexValue(name:String, entityType:EntityType.Value) = name + "-" + entityType.toString

	def cleanupGraphDb() = {
		val tx = graphDb.beginTx()
		try
		{
		    val globalOps = GlobalGraphOperations.at(graphDb)
		    globalOps.getAllRelationships().foreach( r => r.delete() )
		    globalOps.getAllNodes().foreach( r => r.delete() )
		    tx.success()
		}
		finally
		{
		    tx.finish()
		}
	}

	private def registerShutdownHook() = {
	    Runtime.getRuntime().addShutdownHook( new Thread()
	    {
	        override def run() = { graphDb.shutdown() }
	    })
	}

	def shutdownGraphDb() = {
		graphDb.shutdown()
	}
}

object Neo4jEntity {
	val NAME_PROPERTY = "name"
	val TYPE_PROPERTY = "type"
        val READER_PROPERTY = "reader"
}

class Neo4jEntity private[graphdb](aNode: Node) extends Entity {
	var node:Node = aNode

	def name = node.getProperty(Neo4jEntity.NAME_PROPERTY).toString
	def entityType = EntityType.withName(node.getProperty(Neo4jEntity.TYPE_PROPERTY).toString)
        def reader = node.getProperty(Neo4jEntity.READER_PROPERTY).toString

	def neo4jNode = node

	override def toString():String = {
		new org.apache.commons.lang.builder.ToStringBuilder(this)
			.append("name", name)
			.append("type", entityType.toString)
                        .append("reader", reader)
			.toString
	}

	override def hashCode():Int = {
		new org.apache.commons.lang.builder.HashCodeBuilder()
			.append(name)
			.append(entityType.toString)
                        .append(reader)
			.toHashCode
	}

	override def equals(that: Any) = that match { 
		   case other: Neo4jEntity => {
				new org.apache.commons.lang.builder.EqualsBuilder()
					.append(name, other.name)
					.append(entityType, other.entityType)
                                        .append(reader, other.reader)
					.isEquals
		   }
		   case _ => false 
	}
}
