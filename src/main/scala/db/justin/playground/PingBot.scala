package db.justin.playground

import java.util.concurrent.ThreadLocalRandom

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata.{DistributedData, ORSet, ORSetKey}

import scala.concurrent.duration._
import scala.language.reflectiveCalls

object PingBot {
  case object Ping
}

class PingBot extends Actor with ActorLogging {
  import PingBot._

  val pingReplicator   = DistributedData(context.system).replicator
  implicit val cluster = Cluster(context.system)

  // SCHEDULER
  import context.dispatcher
  val pingScheduler = context.system.scheduler.schedule(5.seconds, 5.seconds, self, Ping)

  val DataKey = ORSetKey[String]("data-key")

  pingReplicator ! Subscribe(DataKey, self)

  override def receive: Receive = {
    case Ping =>
      val s = ThreadLocalRandom.current().nextInt(97, 123).toChar.toString
      if(ThreadLocalRandom.current().nextBoolean()) {
        // add
        log.info("Adding: {}", s)
        pingReplicator ! Update(DataKey, ORSet.empty[String], WriteAll(timeout = 5 seconds))(_ + s)
      } else {
        // remove
        log.info("Removing: {}", s)
        pingReplicator ! Update(DataKey, ORSet.empty[String], WriteAll(timeout = 5 seconds))(_ - s)
      }
    case ur: UpdateResponse[_] => log.info("Update Response: {}", ur)
    case changed @ Changed(DataKey) =>
      val data = changed.get(DataKey)
      log.info("Current elements: {}", data.elements)
  }

  override def postStop(): Unit = { super.postStop(); pingScheduler.cancel() }
}
