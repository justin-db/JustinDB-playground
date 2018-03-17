package db.justin.playground

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator.{ReadLocal, WriteLocal}
import akka.cluster.ddata.{DistributedData, LWWMap, LWWMapKey, Replicator}

object KeyValueCache {

  def props: Props = Props[KeyValueCache]

  case class PutInCache(key: String, value: Int)
  case class GetFromCache(key: String)
  case class Cached(key: String, valueOpt: Option[Set[Int]])
  case class Evict(key: String)
}

class KeyValueCache extends Actor with ActorLogging {

  implicit val cluster = Cluster(context.system)
  val replicator = DistributedData(context.system).replicator

  def dataKey(key: String): LWWMapKey[String, Set[Int]] = LWWMapKey("cache-" + scala.math.abs(key.hashCode() % 100))

  override def receive: Receive = {
    // put
    case KeyValueCache.PutInCache(key, value) =>
     replicator ! Replicator.Update(dataKey(key), LWWMap(), WriteLocal) {
       data =>
         val modifiedData = data.get(key).map(_.asInstanceOf[Set[Int]])
         val updatedData = modifiedData.map(Set(value) ++ _).getOrElse(Set(value))
         data + (key -> updatedData)
     }

    // get
    case KeyValueCache.GetFromCache(key) =>
      replicator ! Replicator.Get(dataKey(key), ReadLocal, Some((key, sender())))
    case Replicator.NotFound(_, Some((key: String, replyTo: ActorRef))) =>
      replyTo ! KeyValueCache.Cached(key, None)
    case g @ Replicator.GetSuccess(LWWMapKey(_), Some((key: String, replyTo: ActorRef))) =>
      g.dataValue match {
        case data: LWWMap[_, _] => data.asInstanceOf[LWWMap[String, Set[Int]]].get(key) match {
          case Some(value) => replyTo ! KeyValueCache.Cached(key, Some(value))
          case None        => replyTo ! KeyValueCache.Cached(key, None)
        }
      }

    // evict
    case KeyValueCache.Evict(key) =>
      replicator ! Replicator.Update(dataKey(key), LWWMap(), WriteLocal)(_ - key)
  }
}
