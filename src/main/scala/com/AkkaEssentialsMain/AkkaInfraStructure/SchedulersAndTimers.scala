package com.AkkaEssentialsMain.AkkaInfraStructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.language.postfixOps

//Purpose Of Schedulers And Timers:
//A)To Be Able To Run Code At Defined Point In The Future
//B)May Or MayNot Be Repeated run Code second from now every 3 seconds
object SchedulersAndTimers extends App {
  class ExampleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case msg => log.info(msg.toString)
    }
  }
  val actorSystem = ActorSystem("SchedulersTimersDemo")
  val exampleActor = actorSystem.actorOf(Props[ExampleActor](), "ExampleActor")
  //logger that is used to call info debug,error and warning methods
  actorSystem.log.info("Scheduling For Example Actor")
  //then write first schedule
  //we can call dispatcher  explicitly like this commented code
  //implicit val  executionContext = actorSystem.dispatcher
  //or better import
  import actorSystem.dispatcher
  actorSystem.scheduler.scheduleOnce(1 second) {
    //scheduling of messages like futures need execution context implicit and active thread for
    //being able to execute the code In Akka we use actorsystem.dispatcher Which is a type of execution context
    exampleActor ! "Notifier"
  } //(actorSystem.dispatcher)
  //prints:
  //[akka.actor.ActorSystemImpl(SchedulersTimersDemo)] Scheduling For Example Actor
  //then after 1 second
  //[akka://SchedulersTimersDemo/user/ExampleActor] Notifier
  //==============================================================================
  //schedule;e is deprecated use:
  //A)===>scheduleWithFixedRate a repeated message pass 2 parameters
  //first one initial state second one interval
  //  takes new Runnable as anonymous function
  val repeatedMessage: Cancellable = actorSystem.scheduler.scheduleAtFixedRate(1 second, 2 second) {
    () => exampleActor ! "repeated Message"
  }
  //B)===>ScheduleWithFixedDelay
  val fixedDelay: Cancellable = actorSystem.scheduler.scheduleWithFixedDelay(
    2 second, //initial delay
    3 second, //interval
    exampleActor, //actorRef
    "FixedDelayMessage" //desired message
  )
  //both {{.scheduleAtFixedRate() &&& .scheduleWithFixedDelay()}} of type Cancellable i.e can be canceled
  actorSystem.scheduler.scheduleOnce(4 second) {
    repeatedMessage.cancel()
  }
  actorSystem.scheduler.scheduleOnce(9 second) {
    fixedDelay.cancel()
  }
  //Things To Take In Consideration When Working With Schedules:
  //A)don't use unstable reference when inside scheduling Actors
  //B)All Scheduling Tasks Execute When System Is Terminated Regardless of the initial delay
  //c)Schedulers are not the best at precision and long-term planning like months or more
  //-------------------------------------------------------------------------------------------

  /** Exercise: implement a self-closing actor
   *
   * - if the actor receives a message (anything), you have 1 second to send it another message
   * - if the time window expires, the actor will stop itself
   * - if you send another message, the time window is reset
   */
  class SelfClosingActor extends Actor with ActorLogging {
    var scheduling: Cancellable = createTimeOutWindow()
    def createTimeOutWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second) {
        self ! "TimeOut"
      }
    }
    override def receive: Receive = {
      case "TimeOut" =>
        log.info("Stopping Myself")
        context.stop(self)
      case msg =>
        log.info(s"Received $msg Msg From Self Closing Actor")
        scheduling.cancel()
        scheduling = createTimeOutWindow()
    }
  }
  val selfClosingActor = actorSystem.actorOf(Props[SelfClosingActor](),"SelfClosingActor")
  //selfClosingActor ! "Hi" //[akka://SchedulersTimersDemo/user/SelfClosingActor] Received Hi Msg From Self Closing Actor
  //selfClosingActor ! "TimeOut" //[akka://SchedulersTimersDemo/user/SelfClosingActor#43139920] was not delivered. [1] dead letters
  //for scheduling messages and monitor behavior
  actorSystem.scheduler.scheduleOnce(150 milli){
    selfClosingActor ! "Send Msg Once" //[akka://SchedulersTimersDemo/user/SelfClosingActor] Received Send Msg Once Msg From Self Closing Actor
  }
  //msg will never reach because it exceed the timeOut Window
  actorSystem.scheduler.scheduleOnce(2 seconds){
    selfClosingActor ! "Unreachable" ///user/SelfClosingActor#1631648722] was not delivered. [1] dead letters encountered
  }
  //============================================================================================
  /*Akka Has Built up Utility For send message to itself Called Timer*/
  case object TimerKey
  case object Start
  case object Reminder
  case object Stop
  class TimerBasedHeartbeatActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 500 millis)
    override def receive: Receive = {
      case Start =>
        log.info("Bootstrapping")
        timers.startTimerAtFixedRate(TimerKey, Reminder, 1 second)
      case Reminder =>
        log.info("I am alive")
      case Stop =>
        log.warning("Stopping!")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }
  val timerHeartbeatActor = actorSystem.actorOf(Props[TimerBasedHeartbeatActor](), "timerActor")
  actorSystem.scheduler.scheduleOnce(5 seconds) {
    timerHeartbeatActor ! Stop
  }
}
