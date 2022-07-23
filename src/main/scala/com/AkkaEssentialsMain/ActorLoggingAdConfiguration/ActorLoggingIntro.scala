package com.AkkaEssentialsMain.ActorLoggingAdConfiguration

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}

object ActorLoggingIntro extends App {
  //Actor logging used to show information of running actors so we can debug them
  //which is crucial for distributed system in general
  //4 levels of logging
  //a)DEBUG -> shows exactly what happened in application
  //b)INFO -> most used where benign messages resides
  //c)WARN  -> like DEAD LETTER messages when something goes wrong but not a crashing error
  //4)ERROR --> source a trouble like throwing an exception
  class ExplicitActorLogger extends Actor{
    //Method of logging
    //1)explicit logger:
    //   a)create logging adaptor
    val log: LoggingAdapter = Logging(context.system,this)
    override def receive: Receive = {
      case msg => log.info(msg.toString)
    }
  }
  val actorSystem = ActorSystem("Logging")
  val loggingActor = actorSystem.actorOf(Props[ExplicitActorLogger](),"ActorLogger")
  loggingActor ! "logging A Message"
  //prints
  //[INFO] --> logging level 2
  // [07/23/2022 15:00:39.946] --> time of execution
  // [Logging-akka.actor.default-dispatcher-6] --> Actor System + Message Dispatcher
  // [akka://Logging/user/ActorLogger] --> Actor source message
  // logging A Message -->  printing a message
  //---------------------------------------------------------
  //2) ActorLogging (Most Used)
  class AkkaActorLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      //interpolating parameters
      case (a,b) => log.info("{} {}" ,a,b)
      case message => log.info(message.toString)
    }
  }
  val actorLoggingTrait = actorSystem.actorOf(Props[AkkaActorLogging](),"ActorLogging")
  actorLoggingTrait ! "From Actor Logging Trait"
  //prints
  //[INFO] [07/23/2022 15:21:28.469] [Logging-akka.actor.default-dispatcher-4] [akka://Logging/user/ActorLogging] From Actor Logging Trait
  actorLoggingTrait ! (44,78)
  //[INFO] [07/23/2022 15:26:45.598] [Logging-akka.actor.default-dispatcher-5] [akka://Logging/user/ActorLogging] 44 78
  // IMPORTANT NOTES:
  // logging is done asynchronously
  //akka logging is done by actors
  //you can use any logger implementation like slf4j
}
