package db.justin.playground

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.management.AkkaManagement
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends App with StrictLogging {

  implicit val actorSystem = ActorSystem("ClusterSystem")
  implicit val cluster = Cluster(actorSystem)

  actorSystem.actorOf(KeyValueCache.props)

  AkkaManagement(actorSystem).start().andThen {
    case scala.util.Success(uri) => logger.info("Akka Cluster Management Uri {}", uri)
    case scala.util.Failure(ex) => logger.warn("Failed to start Akka Management", ex)
  }

  logger.info("System has started working...")
}
