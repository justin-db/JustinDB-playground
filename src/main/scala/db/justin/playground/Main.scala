package db.justin.playground

import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

object Main extends App with StrictLogging {

  implicit val actorSystem = ActorSystem("ClusterSystem")
  implicit val cluster = Cluster(actorSystem)

  actorSystem.actorOf(KeyValueCache.props)

  logger.info("System has started working...")
}
