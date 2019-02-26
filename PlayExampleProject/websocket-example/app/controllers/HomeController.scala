package controllers

import java.util.concurrent.TimeUnit

import play.api.mvc._
import play.api.libs.streams.ActorFlow
import javax.inject.{Inject, Singleton}
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink}
import akka.stream.{ActorMaterializer, KillSwitches, UniqueKillSwitch}
import akka.actor._

import scala.concurrent.duration.FiniteDuration

object MyWebSocketActor {
  def props(out: ActorRef) = Props(new MyWebSocketActor(out))
}

class MyWebSocketActor(out: ActorRef) extends Actor {
  def receive = {
    case msg: String =>
      out ! ("I received your message: " + msg)
  }
}
object DynamicStream {
  implicit val as = ActorSystem()
  implicit val mat = ActorMaterializer()
  lazy val publishSubscribeFlow: Flow[String, String, UniqueKillSwitch] = {
    val (sink, source) = MergeHub.source[String](perProducerBufferSize = 16)
      .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
      .run()
    source.runWith(Sink.ignore)
    val busFlow: Flow[String, String, UniqueKillSwitch] = Flow.fromSinkAndSource(sink, source).joinMat(KillSwitches.singleBidi[String, String])(Keep.right)
      .backpressureTimeout(FiniteDuration(3, TimeUnit.SECONDS))
    busFlow
  }
}
class Application @Inject()(cc:ControllerComponents) (implicit system: ActorSystem, mat: Materializer) extends AbstractController(cc) {
  def socket = WebSocket.accept[String, String] { request =>
    ActorFlow.actorRef { out =>
      MyWebSocketActor.props(out)
    }
  }
  def dynamic = WebSocket.accept[String, String] { request =>
    DynamicStream.publishSubscribeFlow
  }
}
