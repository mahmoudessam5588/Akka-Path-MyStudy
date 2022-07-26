package com.AkkaEssentialsMain.ActorTesting

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import scala.concurrent.duration.*

class SynchronousTestingSpec extends AnyWordSpecLike with BeforeAndAfterAll  {
  implicit val actorSystem: ActorSystem = ActorSystem("SynchronousTesting")
  override def afterAll(): Unit = {
    actorSystem.terminate()
  }
  //unit testing is our predictability
  //although asynchronous testing does most of the job but with increasing actor complexity
  //asynchronous testing was become so complicated and frustrating
  //that's why synchronous testing come in handy
  //what does synchronous testing offers??
  //when we send a message to an actor you basically are sure that the actors has already
  //received the that message
  import SynchronousTestingSpec.*
  "A Counter" should{
  "a synchronously increase  it's counter" in{
    val counter = TestActorRef[Counter](Props[Counter]())
    counter ! Inc //counter already receive the message
    //I can already access the instance of an actor
    //test actor ref working on a calling thread
    assert(counter.underlyingActor.count == 1)
  }
    //can invoke the receive handler on underlying actor directly
    "synchronously increase it's counter at the call of the receive function" in{
      val counter = TestActorRef[Counter](Props[Counter]())
      //instead sending a message we can do invoke the receive method
      counter.receive(Inc)
      assert(counter.underlyingActor.count == 1)
    }
    "Working in calling thread dispatcher" in {
      //calling Thread dispatcher --> means communication with the actor happens on the calling thread
      val dispatcherCounter = actorSystem.actorOf(Props[Counter]().withDispatcher(CallingThreadDispatcher.Id))
      val testProbe = TestProbe()
      //the below interaction is already happen on the dispatcher thread so we expect msg zero
      testProbe.send(dispatcherCounter, Read)
      testProbe.expectMsg(Duration.Zero,0)
    }
  }
}
/*Summary:
AsynchronousTesting:All tests are handled on the calling thread by two ways:
A)TestActorRef ==>val counter = TestActorRef[Counter](Props[Counter]()) <-- remember to make actor syatem implicit
B)CallingThreadDispatcher ==>
    val dispatcherCounter = actorSystem.actorOf(Props[Counter]().withDispatcher(CallingThreadDispatcher.Id))
*/



object SynchronousTestingSpec {
  case object Inc
  case object Read
  class Counter extends Actor {
    var count = 0
    override def receive: Receive = {
      case Inc => count += 1
      case Read => sender() ! count
    }
  }
}
