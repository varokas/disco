import play.api._

object Global extends GlobalSettings {
  override def onStop(app: Application) {
    com.huskycode.disco.graphdb.GraphDBService.shutdownGraphDb()
  }
}
