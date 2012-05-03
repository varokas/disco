import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "disco"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "org.neo4j" % "neo4j" % "1.7",
      "org.apache.commons" % "commons-lang3" % "3.0"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
