package com.AkkaEssentialsMain.ActorTesting

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.*
import scala.language.postfixOps
import scala.util.Random

class TimedBoxedAssertionSpec
  extends TestKit(ActorSystem("TimedAssertion", ConfigFactory.load().getConfig("defaultTimedAssertionConfig")))
    with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  //we used TImedBoxed Assertions In Two Cases:
  //a) actors that take big time for hard computation or waiting for resources
  //b) actors that request and reply in rapid or slow successions

  import TimedBoxedAssertionSpec.*

  "A String Actor Handler" should {
    val stringActorHandler = system.actorOf(Props[StringActorHandler]())
    "Reply With String With Timely Manner" in {
      //within()take 2 durations and partial function
      //where string handler actor Instance trigger the action and then we expect the result
      within(500 millis, 3 second) {
        //timed box test that code must execute and finish between 500 to 3 sec
        stringActorHandler ! "InvokeString"
        expectMsg(StringManipulateResult("aaabbbccc"))
      }
    }
    "Reply With String In Sequence timely Manner" in {
      within(1 second) {
        stringActorHandler ! "InvokeStringSequence"
        //as we waiting multiple strings in rapid sequence we will use receive while
        //we specify the type and first parameter => max time
        //second parameter => idle time (interval)
        //third parameter => number (n) Int of messages
        //we open a partial function
        //what of we decrease the first parameter to 500 millis ==> the test will fail because we will not receive all the messages
        //same with second parameter idle time
        //same with number of messages
        val results: Seq[String] = receiveWhile[String](3 second, 700 millis, 10) {
          case StringManipulateResult(text) => text //this will give back sequence of these results
          //so we will assign val results to collect these texts
        }
        //we will check if we aggregate the results in the sequence by their indices number
        assert(results.length > 9)
      }
    }
    //when working with test probe and timed box assertion we can still override the given time in within
    //by adding default time at application.conf configuration
    "Reply with String Using Test Probe In Timely Manner" in {
      within(1 second) {
        //test probe will not care about the boxed assertion due to a configuration was provided to it
        //shown below
        /*#Timed assertion Config
        defaultTimedAssertionConfig{
        akka.test.single-expect-default = 0.4s
        }*/
        //and path provided above in the TestKit
        val testProbe = TestProbe()
        testProbe.send(stringActorHandler, "InvokeString")
        testProbe.expectMsg(StringManipulateResult("aaabbbccc")) //override the box assertion given time
        //with timeout configured to 0.4s so the tes will fail
        //test kit prints the following:
        //assertion failed: timeout (400 milliseconds) during expectMsg while waiting for StringManipulateResult(aaabbbccc)
      }
    }
  }
}


object TimedBoxedAssertionSpec {
  case class StringManipulateResult(text: String)

  class StringActorHandler extends Actor {
    override def receive: Receive = {
      //simulate  actor take long time for hard computation
      case "InvokeString" =>
        //long computation simulation
        Thread.sleep(500)
        sender() ! StringManipulateResult("CCCAAABBB".sortBy(identity).toLowerCase)
      //actors that reply in rapid successions
      case "InvokeStringSequence" =>
        val random = new Random()
        for (_ <- 1 to 10) {
          Thread.sleep(random.nextInt(50))
          sender() ! StringManipulateResult("cccbbbaaa".sortBy(identity).toUpperCase)
        }
      case _ => println("not matched")
    }
  }
}
