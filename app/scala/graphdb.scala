package com.huskycode.disco.graphdb

import com.huskycode.disco.types._

import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb._
import org.neo4j.tooling.GlobalGraphOperations

import scala.collection.JavaConversions._

object GraphDBService extends GraphDBService {
}

object RelTypes extends Enumeration {
  type RelTypes = Value
  val DEPENDS_ON = Value
  
  import org.neo4j.graphdb.RelationshipType
  implicit def conv(rt: RelTypes) = new RelationshipType() {def name = rt.toString}
}

class GraphDBService(val graphDbDir:String) {
  import RelTypes._

	def this() = this("target/db/graphDb")

	val ENTITY_INDEX_KEY = "entity"
	val TYPE_INDEX_KEY = "type"
    val READER_INDEX_KEY = "reader" 

	val graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(graphDbDir);
	val index = graphDb.index().forNodes("nodes") 
	registerShutdownHook()

	def createEntity(name:String, entityType:EntityType.Value, reader:String, content:String):Entity = {
		var entity:Neo4jEntity = null
		val tx = graphDb.beginTx()
		try
		{
			val node = graphDb.createNode()
			node.setProperty(Neo4jEntity.NAME_PROPERTY, name)
			node.setProperty(Neo4jEntity.TYPE_PROPERTY, entityType.toString)
                        node.setProperty(Neo4jEntity.READER_PROPERTY, reader)
                        node.setProperty(Neo4jEntity.CONTENT_PROPERTY, content)

			entity = new Neo4jEntity(node)
			index.add(entity.neo4jNode, ENTITY_INDEX_KEY, getIndexValue(name, entityType))
			index.add(entity.neo4jNode, TYPE_INDEX_KEY, entityType.toString)
                        index.add(entity.neo4jNode, READER_INDEX_KEY, reader)
			tx.success()
		}
		finally
		{
		    tx.finish()   
		}
		return entity
	}

        def createDependency(from: Entity, to: Entity) = {
          val fromEntity = toNeo4jEntity(from) 
          val toEntity = toNeo4jEntity(to)
          
          val tx = graphDb.beginTx() 
          try { 
            fromEntity.neo4jNode.createRelationshipTo(toEntity.neo4jNode, RelTypes.DEPENDS_ON)
            tx.success() 
          } 
          finally { 
            tx.finish()
          } 
        }

        private def toNeo4jEntity(e:Entity) = { 
          e match { 
             case x:Neo4jEntity => x
             case _ => throw new IllegalArgumentException("Entity is not neo4j entity: " + e.toString)
          } 
        }

	def getEntity(name:String, entityType:EntityType.Value):Entity = {
		val result = index.get(ENTITY_INDEX_KEY, getIndexValue(name, entityType)).getSingle
		return if(result != null) new Neo4jEntity(result) else null
	}

	def getEntities(entityType:EntityType.Value):List[Entity] = {
		val result = index.get(TYPE_INDEX_KEY, entityType.toString)
                return getResultList(result)
	}

	def getEntitiesByReader(reader: String):List[Entity] = {
		val result = index.get(READER_INDEX_KEY, reader)
                return getResultList(result)
	} 

	def getDependOnEntites(name:String, entityType:EntityType.Value):List[Entity] = {
		val aNode = index.get(ENTITY_INDEX_KEY, getIndexValue(name, entityType)).getSingle
		return if(aNode != null) {
          val tx = graphDb.beginTx() 
          try { 
            val result = aNode.getRelationships(RelTypes.DEPENDS_ON, Direction.OUTGOING) 
            tx.success() 
            return result.map{ rel => new Neo4jEntity(rel.getEndNode()) }.toList
          } 
          finally { 
            tx.finish()
          } 
			
		}
		else {
			List()
		}
	}
 
        private def getResultList(result: Iterator[org.neo4j.graphdb.Node]) = {
		var lb = new scala.collection.mutable.ListBuffer[Neo4jEntity]()
		while(result.hasNext()) {
			lb += new Neo4jEntity(result.next())
		}

		lb.toList
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
        val CONTENT_PROPERTY = "content"
}

class Neo4jEntity private[graphdb](aNode: Node) extends Entity {
	var node:Node = aNode

	def name = node.getProperty(Neo4jEntity.NAME_PROPERTY).toString
	def entityType = EntityType.withName(node.getProperty(Neo4jEntity.TYPE_PROPERTY).toString)
        def reader = node.getProperty(Neo4jEntity.READER_PROPERTY).toString
        def content = node.getProperty(Neo4jEntity.CONTENT_PROPERTY).toString

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
                        .append(content)
			.toHashCode
	}

	override def equals(that: Any) = that match { 
		   case other: Neo4jEntity => {
				new org.apache.commons.lang.builder.EqualsBuilder()
					.append(name, other.name)
					.append(entityType, other.entityType)
                                        .append(reader, other.reader)
                                        .append(content, other.content)
					.isEquals
		   }
		   case _ => false 
	}
}
