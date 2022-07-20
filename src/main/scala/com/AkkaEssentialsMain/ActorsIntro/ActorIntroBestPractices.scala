package com.AkkaEssentialsMain.ActorsIntro

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorIntroBestPractices extends App{
//Question One How To Instantiate  new classes inside Props
case class FullName(name:String) extends Actor {
  override def receive: Receive = {
    case "Invoke" => println(s"$name")
    case _ =>
  }
}
  val actorsystem: ActorSystem = ActorSystem()
  //we solve with problem by using apply method of props like the following
  //but this is not the best practice the best practice
  // is to create case class and companion objects
  //that have method of the argument you want to pass or display
  //and returns the prop method that carry the new class instance
  //so we comment this line below
  //val myName: ActorRef = actorsystem.actorOf(Props(FullName("Mahmoud")))
  //and do this instead
  object FullName{
    def props(name:String):Props = Props(new FullName(s"$name"))
  }
  val myName :ActorRef = actorsystem.actorOf(FullName.props("Mahmoud"))
  myName ! "Invoke"
}
