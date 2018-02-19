package db.justin.playground

import java.util.concurrent.ThreadLocalRandom

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata.{DistributedData, ORSet, ORSetKey, Replicator}

import scala.concurrent.duration._
import scala.language.reflectiveCalls

object PingBot {
  case object Ping
  case object GetState
}

class PingBot extends Actor with ActorLogging {
  import PingBot._

  val pingReplicator   = DistributedData(context.system).replicator
  implicit val cluster = Cluster(context.system)

  // SCHEDULER
  import context.dispatcher
  val pingScheduler = context.system.scheduler.schedule(5.seconds, 5.seconds, self, Ping)
  val getCurrentStateScheduler = context.system.scheduler.schedule(10.seconds, 7.seconds, self, GetState)

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
    case GetState => pingReplicator ! Get(DataKey, ReadAll(timeout = 5 seconds))
    case gs @ Replicator.GetSuccess(DataKey, req) =>
      val state = gs.get(DataKey).elements
      log.info("Current state of the system {}", state)
    case changed @ Replicator.Changed(DataKey) =>
      val data = changed.get(DataKey)
      context.system.eventStream.publish(EventSubscriberLogger.Log(s"Current elements: ${data.elements}"))
  }

  override def postStop(): Unit = { super.postStop(); pingScheduler.cancel() }
}

class EventSubscriberLogger extends Actor with ActorLogging {

  context.system.eventStream.subscribe(self, classOf[EventSubscriberLogger.Log])

  override def receive: Receive = {
    case EventSubscriberLogger.Log(msg) => log.info("Msg: {}", msg)
  }
}

object EventSubscriberLogger {
  case class Log(msg: String)
}