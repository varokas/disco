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
	val ALL_INDEX_KEY = "all"

	val graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(graphDbDir);
	var index = graphDb.index().forNodes("nodes") 
	registerShutdownHook()

	def createEntity(name:String, reader:String, content:String):Entity = {
		var entity:Neo4jEntity = null
		val tx = graphDb.beginTx()
		try
		{
			val node = graphDb.createNode()
			node.setProperty(Neo4jEntity.NAME_PROPERTY, name)
	        node.setProperty(Neo4jEntity.READER_PROPERTY, reader)
	        node.setProperty(Neo4jEntity.CONTENT_PROPERTY, content)

			entity = new Neo4jEntity(node)
			index.add(entity.neo4jNode, ENTITY_INDEX_KEY, getIndexValue(name, reader))
			index.add(entity.neo4jNode, ALL_INDEX_KEY, ALL_INDEX_KEY)
			tx.success()
		}
		finally
		{
		    tx.finish()   
		}
		return entity
	}

	def deleteEntity(entity:Entity) = {
		val neo4jEntity = toNeo4jEntity(entity)
	      val tx = graphDb.beginTx() 
	      try { 
	      	val node = neo4jEntity.neo4jNode
	        index.remove(node, ENTITY_INDEX_KEY, getIndexValue(neo4jEntity.name, neo4jEntity.reader))
			index.remove(node, ALL_INDEX_KEY, ALL_INDEX_KEY)

	        neo4jEntity.neo4jNode.delete()

	        tx.success() 
	      } 
	      finally { 
	        tx.finish()
	      } 

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

	def getEntity(name:String, reader:String):Entity = {
		val result = index.get(ENTITY_INDEX_KEY, getIndexValue(name, reader)).getSingle
		return if(result != null) new Neo4jEntity(result) else null
	}

	def getEntities():List[Entity] = {
		val result = index.get(ALL_INDEX_KEY, ALL_INDEX_KEY)
        return getResultList(result)
	}

	private def getDependencies(name:String, reader:String , d:Direction):List[Entity] = {
		val aNode = index.get(ENTITY_INDEX_KEY, getIndexValue(name, reader)).getSingle
		return if(aNode != null) {
          val tx = graphDb.beginTx() 
          try { 
            val result = aNode.getRelationships(RelTypes.DEPENDS_ON, d) 
            tx.success() 
            return result.map{ rel => new Neo4jEntity(if (d==Direction.OUTGOING) rel.getEndNode() else rel.getStartNode() ) }.toList
          } 
          finally { 
            tx.finish()
          } 
			
		}
		else {
			List()
		}
	} 

	def getDependByEntites(name:String, reader:String):List[Entity] = {
		getDependencies(name, reader, Direction.INCOMING)
	}

	def getDependOnEntites(name:String, reader:String):List[Entity] = {
		getDependencies(name, reader, Direction.OUTGOING)
	}
 
    private def getResultList(result: Iterator[org.neo4j.graphdb.Node]) = {
		var lb = new scala.collection.mutable.ListBuffer[Neo4jEntity]()
		while(result.hasNext()) {
			lb += new Neo4jEntity(result.next())
		}

		lb.toList
    }

	private def getIndexValue(name:String, reader:String) = name + "-" + reader

	def cleanupGraphDb() = {
		val tx = graphDb.beginTx()
		try
		{
		    val globalOps = GlobalGraphOperations.at(graphDb)

		    globalOps.getAllRelationships().foreach( r => r.delete() )
		    getEntities().foreach( deleteEntity(_) )

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
    val READER_PROPERTY = "reader"
    val CONTENT_PROPERTY = "content"
}

class Neo4jEntity private[graphdb](aNode: Node) extends Entity {
	var node:Node = aNode

	def name = node.getProperty(Neo4jEntity.NAME_PROPERTY).toString
    def reader = node.getProperty(Neo4jEntity.READER_PROPERTY).toString
    def content = node.getProperty(Neo4jEntity.CONTENT_PROPERTY).toString

	def neo4jNode = node

	override def toString():String = {
		new org.apache.commons.lang.builder.ToStringBuilder(this)
			.append("name", name)
            .append("reader", reader)
			.toString
	}

	override def hashCode():Int = {
		new org.apache.commons.lang.builder.HashCodeBuilder()
			.append(name)
	        .append(reader)
	        .append(content)
			.toHashCode
	}

	override def equals(that: Any) = that match { 
		   case other: Neo4jEntity => {
				new org.apache.commons.lang.builder.EqualsBuilder()
					.append(name, other.name)
                    .append(reader, other.reader)
                    .append(content, other.content)
					.isEquals
		   }
		   case _ => false 
	}
}
