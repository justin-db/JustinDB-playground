package db.justin.playground

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.management.AkkaManagement
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.scalalogging.StrictLogging
import db.justin.playground.http.HealthCheckRouter

import scala.concurrent.ExecutionContext

object Main extends App with StrictLogging {

  implicit val actorSystem: ActorSystem   = ActorSystem("ClusterSystem")
  implicit val cluster: Cluster           = Cluster(actorSystem)
  implicit val executor: ExecutionContext = actorSystem.dispatcher
  implicit val materializer: Materializer = ActorMaterializer()

  actorSystem.actorOf(KeyValueCache.props)

  AkkaManagement(actorSystem).start().andThen {
    case scala.util.Success(uri) => logger.info("Akka Cluster Management Uri {}", uri)
    case scala.util.Failure(ex) => logger.warn("Failed to start Akka Management", ex)
  }

  val routes = logRequestResult(actorSystem.name) {
    new HealthCheckRouter().routes
  }
  Http()
    .bindAndHandle(routes, "0.0.0.0", 9000)
    .map { binding => logger.info(s"HTTP server started at ${binding.localAddress}") }
    .recover { case ex => logger.error("Could not start HTTP server", ex) }

  logger.info("System has started working...")
}
