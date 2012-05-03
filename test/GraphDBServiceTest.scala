package com.huskycode.disco.graphdb

import com.huskycode.disco.types._

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

class GraphDBServiceSpec extends Specification {
  "createEntity" should {
    "return entity with values defined" in new withGraphDb {
       val entity = graphDbService.createEntity("testNode", EntityType.Table)

       entity.name === "testNode"
       entity.entityType === EntityType.Table
    }

    "able to retrieve it afterwards using getEntity" in new withGraphDb {
       val entity = graphDbService.createEntity("testNode", EntityType.Table)

       val queriedEntity = graphDbService.getEntity("testNode", EntityType.Table)

       queriedEntity.name === "testNode"
       queriedEntity.entityType === EntityType.Table
    }
  }

  "getEntities" should {
    "return entity with selected type" in new withGraphDb {
       val entity1 = graphDbService.createEntity("testNode1", EntityType.Table)
       val entity2 = graphDbService.createEntity("testNode2", EntityType.Table)

       val queriedEntities = graphDbService.getEntities(EntityType.Table)

       queriedEntities must contain(entity1, entity2).only
    }
  }
}

trait withGraphDb extends After  {
  lazy val graphDbService = new GraphDBService("target/db/testGraphDb")
  graphDbService.cleanupGraphDb()

  def after = { graphDbService.shutdownGraphDb() } 
}