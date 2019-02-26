package example.stream

import java.util.concurrent.TimeUnit

import akka.stream._
import akka.stream.scaladsl._
import akka.{Done, NotUsed}
import akka.actor.ActorSystem

import scala.concurrent._
import scala.concurrent.duration.FiniteDuration

object Main extends App {
  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

  val source: Source[Int, NotUsed] = Source(1 to 10)

  val done: Future[Done] = source.throttle(1, FiniteDuration(1, TimeUnit.SECONDS)).runForeach(i => println(i))(materializer)
  implicit val ec = system.dispatcher
  done.onComplete(_ => system.terminate())
}
