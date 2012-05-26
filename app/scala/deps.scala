package com.huskycode.disco.deps

class DepsBuilder {
  import scala.collection.mutable.HashMap

  val lookupMap = new HashMap[String, DepNode]  

  def addNode(from:String, toList:Iterable[String]) = {
    val toDepNodes = toList.map { to => lookupMap.get(to) match {
      case None => addToMap(to) 
      case Some(originalTo) => originalTo    
    }}
    val fromDepNode = lookupMap.get(from) match {
      case None => addToMap(from)
      case Some(from) => from 
    }

    fromDepNode.addChildren(toDepNodes)
  } 

  private def addToMap(name:String):DepNode = {
     val newDepNode = new DepNode(name)  
     lookupMap.put(name, newDepNode)
     return newDepNode
  }

  def getLookupMap() = lookupMap
}

class DepNode(name:String) { 
  val children = new scala.collection.mutable.MutableList[DepNode]

  def addChildren(newChildren:Iterable[DepNode]) = {
    children ++= newChildren
  }

  def getChildren() = children 
  
  override def toString() = name + ": (" + children.toString + ")"
}
 
