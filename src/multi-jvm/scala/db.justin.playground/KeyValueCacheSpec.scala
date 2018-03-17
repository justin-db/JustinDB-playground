package db.justin.playground

import akka.cluster.Cluster
import akka.cluster.ddata.Replicator.ReplicaCount
import akka.cluster.ddata.{DistributedData, Replicator}
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec, MultiNodeSpecCallbacks}
import akka.testkit._
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

object KeyValueCacheSpec extends MultiNodeConfig {
  val node1 = role("node1")
  val node2 = role("node2")
  val node3 = role("node3")

  commonConfig(ConfigFactory.parseString("""
    akka.loglevel = INFO
    akka.actor.provider = "cluster"
    akka.log-dead-letters-during-shutdown = off
    """))
}

class KeyValueCacheSpecMultiJvm1 extends KeyValueCacheSpec
class KeyValueCacheSpecMultiJvm2 extends KeyValueCacheSpec
class KeyValueCacheSpecMultiJvm3 extends KeyValueCacheSpec

class KeyValueCacheSpec extends MultiNodeSpec(KeyValueCacheSpec) with STMultiNodeSpec with ImplicitSender {
  import KeyValueCacheSpec._

  override def initialParticipants: Int = roles.size

  val cluster = Cluster(system)
  val kvCache = system.actorOf(KeyValueCache.props)

  def join(from: RoleName, to: RoleName) = {
    runOn(from) {
      cluster join node(to).address
    }
    enterBarrier(from.name + "-joined")
  }

  "Key-value cache" must {

    "join cluster" in within(40.seconds) {
      join(node1, node1)
      join(node2, node1)
      join(node3, node1)

      awaitAssert {
        DistributedData(system).replicator ! Replicator.GetReplicaCount
        expectMsg(ReplicaCount(roles.size))
      }

      enterBarrier("after-1")
    }

    "replicate cached entry" in within(10.seconds) {
      runOn(node1) {
        kvCache ! KeyValueCache.PutInCache("key", 1)
      }

      awaitAssert {
        val testProbe = TestProbe()
        kvCache.tell(KeyValueCache.GetFromCache("key"), testProbe.ref)
        testProbe.expectMsg(KeyValueCache.Cached("key", Some(Set(1))))
      }

      enterBarrier("after-2")
    }

    "replicate updated cache entry" in within(10.seconds) {
      runOn(node1) {
        kvCache ! KeyValueCache.PutInCache("key-1", 1)
        kvCache ! KeyValueCache.PutInCache("key-1", 2)
      }

      awaitAssert {
        val testProbe = TestProbe()
        kvCache.tell(KeyValueCache.GetFromCache("key-1"), testProbe.ref)
        testProbe.expectMsg(KeyValueCache.Cached("key-1", Some(Set(1,2))))
      }

      enterBarrier("after-3")
    }

    "replicate evicted entry" in within(15.seconds) {
      runOn(node1) {
        kvCache ! KeyValueCache.PutInCache("key-2", 1)
      }

      awaitAssert {
        val testProbe = TestProbe()
        kvCache.tell(KeyValueCache.GetFromCache("key-2"), testProbe.ref)
        testProbe.expectMsg(KeyValueCache.Cached("key-2", Some(Set(1))))
      }

      runOn(node3) {
        kvCache ! KeyValueCache.Evict("key-2")
      }

      awaitAssert {
        val testProbe = TestProbe()
        kvCache.tell(KeyValueCache.GetFromCache("key-2"), testProbe.ref)
        testProbe.expectMsg(KeyValueCache.Cached("key-2", None))
      }

      enterBarrier("after-4")
    }
  }
}

trait STMultiNodeSpec extends MultiNodeSpecCallbacks
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()
}