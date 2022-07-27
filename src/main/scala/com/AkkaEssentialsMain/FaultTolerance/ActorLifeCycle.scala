package com.AkkaEssentialsMain.FaultTolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
//Actor Life Cycle
//a)Actor Instance ==> Have methods such as  receive handler method , may have internal state
//----------------------------------------------------------------
//b)Actor Reference (Incarnation)
//   - created with actorOf - has mailbox and can have receive message -create one actor instance -have unique identifier
//----------------------------------------------------------------
//c)Actor Path ==> space in actor system may or may-not have actor ref inside {{/user/parent/child}}
//---------------------------------------------------------------------
//##actor can be ==> started-> suspended ->resumed ->restarted -> stopped
//                 ***********************************************************
  //Started ==> create A new ActorRef with a Unique Identifier ID at a given path
//                   ************************************************************
  //Suspended ==> the actor ref will enqueue messages in Mailbox but not process more messages
//                   *************************************************************
  //Resume ===> the actor ref continue process more messages
//                   ***************************************************************
  //Restarting process==>
    //1) Actor already have an Instance Active Then
    //2)actor ref is suspended (enqueue but not process)
    //3)swap the actor instance ==>
      //a) old Instance calls {{preRestart()}}
      //b) replace Actor Instance
      //c) new instance calls {{postRestart()}}
      //d) actor resume
      //{{{{{Internal State Is Destroyed In Restart}}}
//                 *********************************************************************
  //Stopping ==> frees the actor ref within the path
    //1) call {{postStop()}}
    //2) all watching Actors receive {{Terminated(ref)}}
    //3) after Stopping Actor Ref released and {{{another actor ref }}} created at the same path have another unique identifier
    //4){{all messages enqueued in actorRef is lost}}}
//               ***************************************************************************

object ActorLifeCycle extends App{
  case object StartWorker
  class LifeCycleActor extends Actor with ActorLogging{
    override def preStart(): Unit = log.info("I'm Starting")
    override def postStop(): Unit = log.info("I'm Stopping")
    override def receive: Receive = {
      case StartWorker =>
        context.actorOf(Props[LifeCycleActor](),"worker")
    }
  }
  val actorSystem = ActorSystem("LifeCycleDemo")
  val bossActor =  actorSystem.actorOf(Props[LifeCycleActor](),"BossActor")
  //bossActor ! StartWorker //triggers LifeCycleActor to create worker Actor
  //bossActor ! PoisonPill
  //prints
  //[akka://LifeCycleDemo/user/BossActor] I'm Starting ==> boss actor started first
  //[akka://LifeCycleDemo/user/BossActor/worker] I'm Starting ==> then the worker actor
  // [akka://LifeCycleDemo/user/BossActor/worker] I'm Stopping ==> worker actor stopped first
  //[akka://LifeCycleDemo/user/BossActor] I'm Stopping ==> then the boss actor
  //------------------------------------------------------------------------------
  // Restart Implemented In-case of failing or throwing an exception
  case object Fail
  case object FailedEmployee
  case object NewCheck
  case object NewCheckedEmployee
  class SupervisedManager extends Actor{
    private val failedEmployee = context.actorOf(Props[Employee](),"Employee")
    override def receive: Receive ={
      case FailedEmployee => failedEmployee ! Fail
      case NewCheckedEmployee => failedEmployee ! NewCheck
    }
  }
  class Employee extends Actor with ActorLogging {
    override def preStart(): Unit = log.info("Supervised Employee Starting")
    override def postStop(): Unit = log.info("Supervised Employee Stopping")
    //preRestart called by old actor instance before it's swapped
    override def preRestart(reason: Throwable, message: Option[Any]): Unit = log.info(s"SuperVised Actor Restarting Because ${reason.getMessage}")
    //postRestart called by new actor instance just has been inserted at restart procedure
    override def postRestart(reason: Throwable): Unit = log.info("Supervised Actor Restarted")

    override def receive: Receive = {
      case Fail =>
        log.warning("Supervised Employee Is Failing")
        throw new RuntimeException("I Have Failed")
      case NewCheck =>
        log.info("New Employee Started Working")
    }
  }
  val supervisorManager = actorSystem.actorOf(Props[SupervisedManager](),"SuperVisedManager")
  supervisorManager ! FailedEmployee
  supervisorManager ! NewCheckedEmployee
  //prints
  //A)[akka://LifeCycleDemo/user/BossActor] I'm Starting

  //B)[akka://LifeCycleDemo/user/SuperVisedManager/Employee] Supervised Employee Starting

  //C) [akka://LifeCycleDemo/user/SuperVisedManager/Employee] Supervised Employee Is Failing

  //D)[ERROR] [07/27/2022 12:58:07.399] [LifeCycleDemo-akka.actor.default-dispatcher-6]
  //            [akka://LifeCycleDemo/user/SuperVisedManager/Employee] I Have Failed

  //E)[akka://LifeCycleDemo/user/SuperVisedManager/Employee] SuperVised Actor Restarting Because I Have Failed

  //F)[akka://LifeCycleDemo/user/SuperVisedManager/Employee] Supervised Actor Restarted

  //G)[akka://LifeCycleDemo/user/SuperVisedManager/Employee] New Employee Started Working
}

