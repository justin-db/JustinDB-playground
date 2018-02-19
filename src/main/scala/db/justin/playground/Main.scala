package db.justin.playground

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory

object Main {

  def main(args: Array[String]): Unit = {
    val port = args(0)

    val config = ConfigFactory
      .parseString(s"""akka.remote.netty.tcp.port=$port""")
      .withFallback(ConfigFactory.load())
    implicit val actorSystem = ActorSystem("ClusterSystem", config)
    implicit val cluster = Cluster(actorSystem)

    actorSystem.actorOf(Props[PingBot], "pingbot")
    actorSystem.actorOf(Props[EventSubscriberLogger], "EventSubscriberLogger")

  }
}
