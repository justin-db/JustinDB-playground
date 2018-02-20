package db.justin.playground

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata.{DistributedData, LWWMap, LWWMapKey, Replicator}
import db.justin.playground.ShoppingCart.{Cart, LineItem}

import scala.concurrent.duration._

object ShoppingCart {

  def props(userId: String) = Props(new ShoppingCart(userId))

  // API
  case object GetCart
  final case class AddItem(item: LineItem)
  final case class RemoveItem(id: String)

  final case class Cart(items: Set[LineItem])
  final case class LineItem(id: String, name: String, quantity: Int)
}

class ShoppingCart(userId: String) extends Actor with ActorLogging {

  val replicator       = DistributedData(context.system).replicator
  implicit val cluster = Cluster(context.system)

  val DataKey = LWWMapKey[String, LineItem]("cart-" + userId)

  replicator ! Subscribe(DataKey, self)

  override def receive: Receive = {
    // get cart
    case ShoppingCart.GetCart =>
      replicator ! Replicator.Get(DataKey, ReadMajority(3.seconds), Some(sender()))
    case g @ Replicator.GetSuccess(DataKey, Some(replyTo: ActorRef)) =>
      replyTo ! Cart(g.get(DataKey).entries.values.toSet)
    case Replicator.NotFound(DataKey, Some(replyTo: ActorRef)) =>
      replyTo ! Cart(Set())
    case Replicator.GetFailure(DataKey, Some(replyTo: ActorRef)) =>
      // ReadMajority failure, try again with local read
      replicator ! Get(DataKey, ReadLocal, Some(replyTo))

    // add item
    case cmd @ ShoppingCart.AddItem(item)  =>
      replicator ! Replicator.Update(DataKey, LWWMap(), WriteMajority(3.seconds), Some(cmd)) { lineItems =>
        lineItems.get(item.id) match {
          case Some(LineItem(_, _, currentQuantity)) => lineItems + (item.id -> item.copy(quantity = currentQuantity + 1))
          case None                                  => lineItems + (item.id -> item)
        }
      }

    // remove item
    case ri @ ShoppingCart.RemoveItem(id) =>
      // To be able to remove element from ORMap, it must to have seen it
      // hence get latest view from majority first
      replicator ! Replicator.Get(DataKey, ReadMajority(3.seconds), Some(ri))
    case Replicator.GetSuccess(DataKey, Some(ShoppingCart.RemoveItem(id))) =>
      replicator ! Replicator.Update(DataKey, LWWMap(), WriteMajority(3.seconds), None) { lineItems =>
        lineItems - id
      }
    case Replicator.NotFound(DataKey, Some(ShoppingCart.RemoveItem(id))) =>
      // ReadMajority failed, fall back to best effort local value
      replicator ! Replicator.Update(DataKey, LWWMap(), WriteMajority(3.seconds), None) { lineItems =>
        lineItems - id
      }
  }
}
