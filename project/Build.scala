import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "disco"
    val appVersion      = "1.0-SNAPSHOT"


    val appDependencies = Seq(
      "org.neo4j" % "neo4j" % "1.7",
      "org.apache.commons" % "commons-lang3" % "3.0",
      "net.sourceforge.jtds" % "jtds" % "1.2.4",
      "net.sourceforge.jregex" % "jregex" % "1.2_01",
      "oracle" % "ojdbc14" % "10.2.0.2"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
      resolvers += "Ibiblio" at "http://mirrors.ibiblio.org/pub/mirrors/maven/mule/dependencies/maven2/"
    )

}
