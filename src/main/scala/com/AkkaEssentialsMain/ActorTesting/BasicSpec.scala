package com.AkkaEssentialsMain.ActorTesting

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import scala.concurrent.duration.*
import scala.language.postfixOps
import scala.util.Random
//Towards Scala Akka BDD (Behavioral Driven Development)
//Recompounded Naming Conventions Of Test Class To end with Spec Prefix
//Next Introduce {{TestKit}} With ActorSystem Parameter
//---------------------------
//ImplicitSender => send reply Scenarios in Actors which are a lot,
//Trait ImplicitSender with TestKitBase extends AnyRef and have value member,
//implicit def self: ActorRef
//----------------------------
//AnyWordSpecLike ==> Implementation trait for class AnyWordSpec,
// which facilitates a “behavior-driven” style of development (BDD),
// in which tests are combined with text that specifies the behavior the tests verify.
//------------------------------
//BeforeAndAfterAll ==> This trait allows code to be executed before and/or after all the tests and nested suites of a suite are run.
// This trait overrides run and calls the beforeAll method, then calls super.run.
// After the super.run invocation completes, whether it returns normally or completes abruptly with an exception,
//====>>>{{{this trait's run method will invoke afterAll.AS SHOWN BELOW}}}
class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {
  //Act like A hook used to destroying and tearing down test suite in a good way :)
  override def afterAll(): Unit = {
    //The setup Flow -> Test Suite Instantiated -> ActorSystem Will Created Naturally -> At The End OF Tear down -> We Will Terminate The ActorSystem
    //This Implemented By
    TestKit.shutdownActorSystem(system) //where system is a member of test kit
  }

  import BasicSpec.*
  //After That Is the Natural Flow Pattern Of BDD ScalaTest
  //Tests at top amd Logic to be tested Against at Bottom
  "The Simple Msg Actor" should {
    "Send Back The Same Msg" in {
      //here we Instantiate Actor of  The class SimpleMsgActor
      val msgActor = system.actorOf(Props[SimpleMsgActor]())
      val msg = "Calling From Test Kit"
      msgActor ! msg
      expectMsg(msg)
    }
  }
  "The Failed Msg Actor" should {
    "Failing To Send Back Msg" in {
      val failedMsgActor = system.actorOf(Props[FailedMsgActor]())
      val failedMsg = "I Failed"
      failedMsgActor ! failedMsg
      //failed after waiting certain amount of time 3s default with expectMsg()
      expectNoMessage(1 second) //but this will pass obviously
      //Important Question need to be addressed who is sending the messages and
      //who is at the receiving End expect these messages??
      //testActor ===> which is a Member of a Test Kit actors used for communication with actors
      //want to be tested {{testActor is passed implicitly as a sender of every single message}}
      //BECAUSE OF =====>>>>>{{{{ImplicitSender trait we added to testKit}}}}
    }
  }
  "Find Char At Actor And String Manipulate Messages" should {
    val filterCharAtAndString = system.actorOf(Props[FindCharAtAndStringManipulate]())
    "Get Char Index Of String" in {
      filterCharAtAndString ! "one"
      //will failed due to default time is set to 3 had to increase the duration to finish computation
      //expectMsg(6 second, "e")
      //now for more constructive testing than expectMsg that use only basic equality
      //using ExpectMsgType[] that get hold of the message for type equality
      //Please Note This Is Not Found vs expected it's only checking type equality
      //not useful if you transform the string AGAIN ONLY CHECKING TYPE
      val msgReply: String = expectMsgType[String]
      //extra safe to give time for computing
      //note that
      assertForDuration(msgReply == "ez", 10 second, 10 second)
    }
    "Either  Greeting Msg reply" in {
      filterCharAtAndString ! "greeting"
      //expect result this or that
      expectMsgAnyOf("hi", "hello")
    }
    "Get All Msg Replies" in {
      filterCharAtAndString ! "fullName"
      expectMsgAllOf("Mahmoud","Essam")
    }
    "Powerful Get All Msg Replies" in{
      filterCharAtAndString ! "fullName"
      expectMsgPF(){
        //allow for more powerful granular assertion than the primitive ones
        //we only care that partial function is defined
        case "Mahmoud" =>
        case "Essam" =>
      }
    }
  }
}

//Best Practice Is to have Companion Object for The Test Suite:
object BasicSpec {
  //The Reason For That To Store All The Value Or Methods Inside The Companion Object You Are Going To Use In Your Test
  class SimpleMsgActor extends Actor {
    override def receive: Receive = {
      case msg => sender() ! msg
    }
  }
  class FailedMsgActor extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }
  class FindCharAtAndStringManipulate extends Actor {
    val random: Random = Random()
    override def receive: Receive = {
      //Notice Placing Of case greeting and fullName in the top of receive massage is crucial
      //to be pattern matcher first otherwise Test Will fail
      case "greeting" =>
        if random.nextBoolean() then sender() ! "Hi"
        else sender() ! "hello"
      case "fullName" =>
        sender() ! "Mahmoud"
        sender() ! "Essam"
      //here if we don't add toString the test will fail because the method will return char 'e'
      //not a string of "e"
      case str: String => sender() ! str.charAt(2).toString
    }
  }
}
