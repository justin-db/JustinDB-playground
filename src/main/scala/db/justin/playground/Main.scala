package db.justin.playground

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import db.justin.playground.ShoppingCart.LineItem

import scala.concurrent.duration._
import scala.util.Random

object Main {

  def main(args: Array[String]): Unit = {
    val port = args(0)

    val config = ConfigFactory
      .parseString(s"""akka.remote.netty.tcp.port=$port""")
      .withFallback(ConfigFactory.load())
    implicit val actorSystem = ActorSystem("ClusterSystem", config)
    implicit val cluster = Cluster(actorSystem)

//    actorSystem.actorOf(Props[PingBot], "pingbot")
//    actorSystem.actorOf(Props[EventSubscriberLogger], "EventSubscriberLogger")

//    ------------
    implicit val timeout = Timeout(5.seconds)
    implicit val ec = actorSystem.dispatcher

    val sc1 = actorSystem.actorOf(ShoppingCart.props("1"), "shopping-cart-1")
    val sc2 = actorSystem.actorOf(ShoppingCart.props("2"), "shopping-cart-2")

    while (true) {
      Thread.sleep(1500)

      // Get Cart
      (sc1 ? ShoppingCart.GetCart).mapTo[ShoppingCart.Cart].foreach { cart =>
        println("shopping-cart-1: " + cart.items)
      }
      (sc2 ? ShoppingCart.GetCart).mapTo[ShoppingCart.Cart].foreach { cart =>
        println("shopping-cart-2: " + cart.items)
      }

      // Add or Remove
      if(Random.nextBoolean()) {
        println("Adding new elements")
        sc1 ! ShoppingCart.AddItem(LineItem(Random.nextInt(5).toString, "milk", 1))
        sc2 ! ShoppingCart.AddItem(LineItem(Random.nextInt(5).toString, "eggs", 10))
      } else {
        println("Possibly remove some elements")
        val times = Random.nextInt(2)
        (0 to times).foreach { _ =>
          sc1 ! ShoppingCart.RemoveItem(Random.nextInt(5).toString)
          sc2 ! ShoppingCart.RemoveItem(Random.nextInt(5).toString)
        }
      }
    }
  }
}
