package db.justin.playground.http

import akka.actor.ActorRef
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{get, _}
import akka.http.scaladsl.server.Route
import akka.pattern.ask
import akka.util.Timeout
import db.justin.playground.KeyValueCache
import spray.json.DefaultJsonProtocol

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class GetPutMessagesRouter(kvActorRef: ActorRef) extends DefaultJsonProtocol {

  private[this] implicit val timeout: Timeout = Timeout(5.seconds)

  case class Message(key: String, value: Int)
  implicit val messageFormat = jsonFormat2(Message)

  val routes: Route = path("data") {
    getRoute ~ postRoute
  }

  private[this] val getRoute = get {
    parameter("key") { id =>
      onComplete(kvActorRef ? KeyValueCache.GetFromCache(id)) {
        case Success(KeyValueCache.Cached(_, Some(values))) => complete(StatusCodes.OK                  -> values.mkString(", "))
        case Success(KeyValueCache.Cached(_, None))         => complete(StatusCodes.NotFound            -> s"Not found values for corresponding key $id")
        case Failure(ex)                                    => complete(StatusCodes.InternalServerError -> ex.getMessage)
      }
    }
  }
  private[this] val postRoute = post {
    entity(as[Message]) { msg =>
      kvActorRef ! KeyValueCache.PutInCache(msg.key, msg.value)
      complete((StatusCodes.Accepted, "key-value placed"))
    }
  }
}
