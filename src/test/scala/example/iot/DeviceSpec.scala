package example.iot

import java.util.concurrent.TimeUnit

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}

import scala.concurrent.duration.FiniteDuration
import scala.language.postfixOps

class DeviceSpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("AkkaQuickstartSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "Device Actor" should {
    "reply with empty reading if no temperature is known" in {
      val probe = TestProbe()
      val deviceActor = system.actorOf(Device.props("group", "device"))

      deviceActor.tell(Device.ReadTemperature(requestId = 42), probe.ref)
      val response = probe.expectMsgType[Device.RespondTemperature]
      response.requestId should ===(42L)
      response.value should ===(None)
    }
    "reply with latest temperature reading" in {
      val probe = TestProbe()
      val deviceActor = system.actorOf(Device.props("group", "device"))

      deviceActor.tell(Device.RecordTemperature(requestId = 1, 24.0), probe.ref)
      probe.expectMsg(Device.TemperatureRecorded(requestId = 1))

      deviceActor.tell(Device.ReadTemperature(requestId = 2), probe.ref)
      val response1 = probe.expectMsgType[Device.RespondTemperature]
      response1.requestId should ===(2L)
      response1.value should ===(Some(24.0))

      deviceActor.tell(Device.RecordTemperature(requestId = 3, 55.0), probe.ref)
      probe.expectMsg(Device.TemperatureRecorded(requestId = 3))

      deviceActor.tell(Device.ReadTemperature(requestId = 4), probe.ref)
      val response2 = probe.expectMsgType[Device.RespondTemperature]
      response2.requestId should ===(4L)
      response2.value should ===(Some(55.0))
    }
    "reply to registration requests" in {
      val probe = TestProbe()
      val deviceActor = system.actorOf(Device.props("group", "device"))

      deviceActor.tell(DeviceManager.RequestTrackDevice("group", "device"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered)
      probe.lastSender should ===(deviceActor)
    }

    "ignore wrong registration requests" in {
      val probe = TestProbe()
      val deviceActor = system.actorOf(Device.props("group", "device"))

      deviceActor.tell(DeviceManager.RequestTrackDevice("wrongGroup", "device"), probe.ref)
      probe.expectNoMessage(FiniteDuration(500, TimeUnit.MILLISECONDS))

      deviceActor.tell(DeviceManager.RequestTrackDevice("group", "Wrongdevice"), probe.ref)
      probe.expectNoMessage(FiniteDuration(500, TimeUnit.MILLISECONDS))
    }
  }
}

