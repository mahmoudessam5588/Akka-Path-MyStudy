package com.AkkaEssentialsMain.FaultTolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

//AkkA Philosophy:
//A)Actor Failure Are Fine
// ------------------------------------------------------------
//B)Supervised Actor Must Decide Upon The Underlying Actor Hierarchy Failure
// --------------------------------------------------------------
//C)If One Underlying Actor Hierarchy Failed Action To Be Taken Should
    //1) Suspends All The Sub Actors underlying that failed Actor
    //2)Send Special Message To Supervised Actor Notifying Of The Failure
//-----------------------------------------------------------------
//D)Supervised Should Take The Following Actions
    //1)Resume The Actor
    //2)Restart The Actor(Default)
    //3)fail Itself and {{{Escalate}}
class SupervisionStrategySpec extends TestKit(ActorSystem("SupervisionSpec"))
with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll
{
  override def afterAll(): Unit ={
    TestKit.shutdownActorSystem(system)
  }
  import SupervisionStrategySpec.*
  "SupervisorActorOfPropsStrategy" should{
    "Resume It's Underlying Actor in case of a minor Fault"in{
    val supervisor = system.actorOf(Props[SupervisorActorOfPropsStrategy]())
    supervisor ! Props[WordCountActor]()
    val actorRef = expectMsgType[ActorRef]
    actorRef ! "Mahmoud Essam"
    actorRef ! StrReportCount //2 words
    expectMsg(2) //test passes
    actorRef ! "My Quote Of Life Never Let Your Mind Breaks The Spirit Of Your Soul"
    //this will trigger a run time exception then will trigger the SupervisedActorOfPropsStrategy
    //to Trigger Resume Strategy So We Should Expect The Same Previous Result Of 2 Words When We
    //Invoke TheStrReportCount
    actorRef ! StrReportCount
    expectMsg(2)  //test passes
    }
    "Restart It's Underlying Actor in case of an Empty String"in{
      val supervisor = system.actorOf(Props[SupervisorActorOfPropsStrategy]())
      supervisor ! Props[WordCountActor]()
      val actorRef = expectMsgType[ActorRef]
      actorRef ! ""
      //this will trigger a null pointer exception then will trigger the SupervisedActorOfPropsStrategy
      //to Trigger Restart Strategy So We Should Expect words count of 0 Words When We Invoke TheStrReportCount
      // because {{Initial State}} internal state has been destroyed and actor instance inside the actor reference is swapped
      actorRef ! StrReportCount
      expectMsg(0)
    }
    "Stopping It's Underlying Actor in case of a Major Error"in{
      val supervisor = system.actorOf(Props[SupervisorActorOfPropsStrategy]())
      supervisor ! Props[WordCountActor]()
      val actorRef = expectMsgType[ActorRef]
      //register actor ref for death watch strategy
      watch(actorRef)
      //in-case str started with lower case
      actorRef ! "mahmoud essam"
      //we expect a terminated message so we need to get hold of reference of this message
      //with expectMsgType[] and then assert it that it's equal to actor ref
      val terminatedMsg: Terminated = expectMsgType[Terminated]
      assert(terminatedMsg.actor == actorRef)//passes
    }
    "Escalate It's Underlying Actor And Supervised Actor in case of It's Out Of Options"in{
      val supervisor = system.actorOf(Props[SupervisorActorOfPropsStrategy]())
      supervisor ! Props[WordCountActor]()
      val actorRef = expectMsgType[ActorRef]
      //make actorRef receive types other than string
      //register to death watch
      watch(actorRef)
      actorRef ! 34
      val terminatedMsg: Terminated = expectMsgType[Terminated]
      assert(terminatedMsg.actor == actorRef) //passes
      //because the escalate strategy kills everything and stop the SupervisedActor which by default restart the wordCount method
      //what if we don't want this behaviour to occur  so lets create an
      //extra supervisor below called {{{NoDeathOnRestartExtraSupervisor}} which extend {{SupervisorActorOfPropsStrategy}}
      //and override {{preRestart()}} method if we look it it's signature below:
      /*def preRestart(@unused reason: Throwable, @unused message: Option[Any]): Unit = {
        context.children.foreach { child =>
          context.unwatch(child)
          context.stop(child)
        }
        postStop()
      }*/
      //for every child actor it unwatched the child and says context.stop(child)
      //so it will stop the child and keep wordCounter alive that the behaviour we want
    }
  }
  "A  Less Aggressive Supervisor Behaviour" should{
    "Not kill Underlying Actors In case It's restarted or Escalates Failure"in{
      val supervisor = system.actorOf(Props[NoDeathOnRestartExtraSupervisor]())
      supervisor ! Props[WordCountActor]()
      val actorRef = expectMsgType[ActorRef]
      actorRef ! "Akka Is Awesome"
      actorRef ! StrReportCount
      expectMsg(3)

      actorRef ! 34
      //here after escalation the supervisor will not kill underlying actors
      actorRef ! StrReportCount
      //back to initial state
      expectMsg(0)//passes
    }
  }
  "An All For Ine Supervisor"should{
    "Apply All For One Strategy"in{
      val supervisor = system.actorOf(Props[AllForOneSupervisorStrategy](),"AllForOneSuperVisor")
      supervisor ! Props[WordCountActor]()
      val actorRefOne = expectMsgType[ActorRef]
        //applied twice
      supervisor ! Props[WordCountActor]()
      val actorRefTwo = expectMsgType[ActorRef]
      actorRefTwo ! "Testing Supervision"
      actorRefTwo ! StrReportCount
      expectMsg(2)

      //wait for the child to throw exception with EventFilter and Intercepting the Exception
      EventFilter[NullPointerException]()intercept{
        //actorRefOne send with empty str caused ===> both of them {{actRefOne And ActRefTwo To Restart}}}
        actorRefOne ! ""
      }
      Thread.sleep(500)//not best practise but test happen so fast that it fails
      //after throwing send report to actorRefTwo
      actorRefTwo ! StrReportCount
      //count returns back from 2 to zero Initial State
      expectMsg(0)
    }
  }
}
object SupervisionStrategySpec{
  case object StrReportCount
  class SupervisorActorOfPropsStrategy extends Actor{
    //defining different supervisor strategy than restart(default)
    //the way is overriding supervisor strategy to handle different kinds of exceptions that wordCounterActor might throw
    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy(){
      //action to be taken in case i receive the exact type of exception or the exact actor that causes the failure
      case _: NullPointerException => Restart //provided by akka called a Directive
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
    override def receive: Receive = {
      case props: Props => val propsRef = context.actorOf(props)
      sender() ! propsRef
    }
  }
  //{{{Alternative For OneForOneStrategy}}}}
  class AllForOneSupervisorStrategy extends SupervisorActorOfPropsStrategy {
    override val supervisorStrategy: SupervisorStrategy = AllForOneStrategy() {
      //applies the below strategy for {{{all actors}}} regardless of that one actor that caused the failure
      //if one Restarts All Actors restarts and so on
      case _: NullPointerException => Restart //provided by akka called a Directive
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
  }
  class NoDeathOnRestartExtraSupervisor extends SupervisorActorOfPropsStrategy{
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
      //leave it empty for the desired behaviour and test it out
    }
  }
  class WordCountActor extends Actor{
    var words = 0
    override def receive: Receive ={
      case StrReportCount => sender() ! words
      case "" =>throw  new NullPointerException("Received An Empty String")
      case str:String =>
        if str.length > 20 then throw new RuntimeException("String Too Long To Handle")
        else if !Character.isUpperCase(str(0)) then throw IllegalThreadStateException("No Accepting String Start With UpperCase")
        else  words += str.split(" ").length
      case _ => throw new Exception("Only Receiving Strings")
    }
  }
}
