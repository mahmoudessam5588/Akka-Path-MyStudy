package com.AkkaEssentialsMain

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorIntro extends App {
  //======>step One:Actors are implemented by extending the Actor base trait
  // and implementing the receive method.
  class ActorClass extends Actor {

    //======>Step Two:The receive method should define a series of case statements
    // {{{{(which has the type PartialFunction[Any, Unit])}}}
    // that define which messages your Actor can handle, using standard Scala pattern matching,
    //along with the implementation of how the messages should be processed.
    override def receive: Receive = // pattern matching logic here to handle actor
    {
      //what are the characteristic of Actors??
      //a)actors are uniquely identified
      //b)Messages are asynchronous
      //c)each actor can && || may respond differently according to given logic
      //d)actor are encapsulated by default
      case input: String => input.foreach(println)
      case _ =>
    }
  }
  //===>step Three: Define The Actor System
  //So What Is Actor System ??
  /*An actor system is a hierarchical group of actors which share common configuration,
   e.g. dispatchers, deployments, remote capabilities and addresses(Will Discuss Them Later).*/
  val actorsystem: ActorSystem = ActorSystem("ActorIntro") // given name must not contain any spaces
  //===> Step Four: creating actor instances
  //by Providing The blow method with Props
  //a)actOf==> creates a new actor as a child with mandatory given name
  //b)props ==> return the calculated value instance
  //both a nd b are equivalent to new keyword in normal OOP
  //the return value of this whole expression is an actor reference {{actorRef}}
  //actorRef ==> Returns the path for instantiated actor (from this actor up to the root actor).
  val actorInstanceSignaler: ActorRef =
          actorsystem.actorOf(Props[ActorClass](), "actorsInstance")
 //====> Step Five: communicate between actor send signal and receive the result
  actorInstanceSignaler ! "My Name Is Mahmoud And I'm Explaining Akka"
  //what happen here that actorInstance Signaler send a message with the type String
  // to the receiver  {{ receive method}} will match the type and send the result
  // of it's computing to the signaler to be displayed in the console
}
