package com.AkkaEssentialsMain.ActorTesting

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import scala.concurrent.duration.*
import scala.language.postfixOps
//What is the purpose Of Test Probes??
//Is special type of actor with assertion capabilities
//Is the last example we used normal asynchronous testing to hook into an actor and test the expected result
//so what if we want to test crucial  behavior of an actor like scheduling tasks that lead to either change of an actor
//Immutable state or for example create worker actor/(s) to supervise them in so in this case we want to test
//both the supervised master actor and his dedicated works along with their behavior that's when Test probes comes in handy
//if you checked my previous posts or my github repo I discussed an Master Actor Worker Hierarchy Where we utilise the
//akka parallelism programming to schedule create and schedule a task for an actors to count a certain Char or Word in big chunk
//of a book and return it's count using Round Robin Logic
// I will simplify the example for demonstration purposes
// and show  who to use test probes
class TestProbeSpec extends TestKit(ActorSystem("TestProbes"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  /*The Work FLow Scenario
* Request work to SupervisedMaster
* SupervisedMaster Create and Schedule workers  For the Task
* A Worker process the work and reply to the master
* Master Display the Result To The Requester*/
  //so in this example the test probe will be as if an entities the hold place of dedicated workers and interact we supervise
  //Master actor to be it's change of state behaviour and work scheduling So lets see how to implement to do that
  import TestProbeSpec.*
  "A Supervised Master Actor" should {
    "Request Work To Worker" in {
    val supervisedMasterActor = system.actorOf(Props[SupervisedMasterActor]())
    val dedicatedWorker = TestProbe("dedicatedWorker")
    supervisedMasterActor ! Request(dedicatedWorker.ref)
    expectMsg(RequestApproved)
    }
    "Send A Work To Worker"in{
      //as you notice we just going with the normal work flow to test each case class one by one
      //to ensure full test of all our given functionality
      val supervisedMasterActor = system.actorOf(Props[SupervisedMasterActor]())
      val dedicatedWorker = TestProbe("dedicatedWorker")
      supervisedMasterActor ! Request(dedicatedWorker.ref)
      expectMsg(RequestApproved)
      val assignRequestedText = "Check Mahmoud Essam Linkedin Profile He is  Open To Work"
      //supervisedMaserActor assign a task {{see the behavioral testing as if natural language"
      supervisedMasterActor ! Task(assignRequestedText)
      //see what the expected action now to happen after we invoke Task
      //If You Check below we expect the worker to start working in the task right??!!
      //so we will test dedicatedWork to do the work and expecting certain result
      //this is an interaction testing between SupervisedMasterActor And DedicatedWorkerActor
      dedicatedWorker.expectMsg(workerWork(assignRequestedText,testActor))
      //just look at the implemented code above and see how intuitive it is
      //So The Super probes of test probes that it has not only the ability to send and test interaction
      //BUT ALSO TO REPLY!!!!
      dedicatedWorker.reply(workCompleted(10,testActor))
      //as we are writing natural language
      //so what's left not tested Right The workerReport
      //so we expect a report here is it
      expectMsg(workerReport(10))
    }
    //so what if we want to test More Than One Task Given To SupervisedMasterActor and test all functionalities like above
    //and Get a  Total WorkerReport we can with receiveWhile() lest see hpw to implement it
    "Send Multiple Tasks To Workers "in{
      val supervisedMasterActor = system.actorOf(Props[SupervisedMasterActor]())
      val dedicatedWorker = TestProbe("dedicatedWorker")
      supervisedMasterActor ! Request(dedicatedWorker.ref)
      expectMsg(RequestApproved)
      val assignRequestedText = "Check Mahmoud Essam Linkedin Profile He is  Open To Work"
      supervisedMasterActor ! Task(assignRequestedText)
      supervisedMasterActor ! Task(assignRequestedText)
      dedicatedWorker.receiveWhile(){
        case workerWork(`assignRequestedText`,`testActor`) =>
          dedicatedWorker.reply(workCompleted(10,testActor))
      }
      expectMsg(workerReport(10))
      //sum two assigned text
      expectMsg(workerReport(20))
    }
  }
}

object TestProbeSpec {
  case class Request(workerRef: ActorRef)
  case object RequestApproved
  case class Task(text: String)
  case class workerWork(text: String, requester: ActorRef)
  case class workCompleted(count: Int, request: ActorRef)
  case class workerReport(totalCount:Int)

  class SupervisedMasterActor extends Actor{
    override def receive: Receive = {
      case Request(workerRef)=>
        sender() ! RequestApproved
        context.become(startWorking(workerRef,0))
      case _ =>
    }
    def startWorking(workerRef: ActorRef, totalCount: Int):Receive={
      case Task(text) => workerRef ! workerWork(text,sender())
      case workCompleted(count,requester) =>
        val newTotalCount = totalCount + count
        requester ! workerReport(newTotalCount)
        context.become(startWorking(workerRef, newTotalCount))
    }
  }
  //expect worker Actor here we will replace that with test probe who will mimic it's behaviour
}
