package com.AkkaEssentialsMain.FaultTolerance

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}
import scala.concurrent.duration.*
import java.io.File
import scala.io.Source
import scala.language.postfixOps

object BackOffSupervisorPattern extends App {
  //Backoff supervision  Pattern solves big problem in practice which is the repeated restart of actors
  //Restarting may cause harm in come cases like when database goes down and many actors trying to read and write
  //at the database many actors will start to throw exceptions and Invoke supervision strategy
  //and if the actors starts at the same time the database might go down again or actors might get into
  //blocking state the backoff pattern provides exponential delay as well as randomness between the attempts
  //to rerun a supervisor strategy

  //create a file with data that mimic a database and a class that tries to read from a file
  case object ReadFile

  class FileBasedPersistenceActor extends Actor with ActorLogging {
    var dataSource: Source = null
    override def preStart(): Unit = log.info("Persistence Actor Starting")
    override def postStop(): Unit = log.info("Persistence Actor Stopping")
    override def postRestart(reason: Throwable): Unit = log.warning("Persistence Actor Restarting")
    override def receive: Receive = {
      case ReadFile =>
        if dataSource == null then
          dataSource = Source.fromFile(new File("src/main/resources/testfiles/important_dataz.txt"))
        log.info(s"Reading From File : ${dataSource.getLines().toList}")
    }
  }

  val actorSystem = ActorSystem("BackOffSupervisorDemo")
  val filPersistenceActor = actorSystem.actorOf(Props[FileBasedPersistenceActor](),"fileBaseActor")
  filPersistenceActor ! ReadFile
  //***************************************************************************************

  //next we change the name of file so the file can't be found
  //we got file n0t found exception and 2 log messages of actor stopping and restarting
  /* [akka://BackOffSupervisorDemo/user/fileBaseActor] Persistence Actor Stopping
[akka://BackOffSupervisorDemo/user/fileBaseActor] Persistence Actor Restarting*/
  //in real life we will got loads of actors trying to restart and read from the database
  //preventing database from recovery and get back up
  //so we must use supervised pattern so shown below
  //A)==>assigned a val of props object
  val backoffActorProps = BackoffSupervisor.props(
    BackoffOpts.onFailure(
      Props[FileBasedPersistenceActor](),
      "SimpleBackOffActor", //child created with name SimpleBackoffActor of type FileBasedPersistenceActor
      3 seconds, //kicks in after 3 seconds then 2x of first failure attempt {3s,6s,12s,24s->then capped at30}
      30 seconds, //the cap of attempt at 30 secs
      0.2 //{randomFactor}add noise to this time so we don't have huge amount of actors starting
      //at the same moment of time
    )
  )
  val simpleBackoffSupervisor = actorSystem.actorOf(backoffActorProps, "simpleSupervisor")
  simpleBackoffSupervisor ! ReadFile
  /*What Happened Above
* A)backoffActorProps ==>
*       created a child {{SimpleBackOffActor}}based on props of type{{FileBasedPersistenceActor}}
* B)backoffActorProps ==> received any message and can forward them to the child actor
* C)The SupervisionStrategy Is the Default One(Restarting On EveryThing)with =>
*   - extra Functionality to it when Child actor Fails {{called on Failure() Method}}
    - Supervision Strategy Kicks In after 3 seconds
    - If ChildActor fails Again it  2x double of the first attempt
 */
  //*********************************************************************************
  //lets create a second back up supervisor which acts on stop and customize the supervision strategy
  val stopSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[FileBasedPersistenceActor](),
      "stopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy( //customize by passing supervision strategy inside
      OneForOneStrategy() {
        case _ => Stop //any type of exception it's going to stop
          //then after 3 seconds backoffOpts kicks in and start the actor
      }
    )
  )
  val stopSupervisor = actorSystem.actorOf(stopSupervisorProps, "stopSupervisor")
  stopSupervisor ! ReadFile

  //********************************************************************************8

}


