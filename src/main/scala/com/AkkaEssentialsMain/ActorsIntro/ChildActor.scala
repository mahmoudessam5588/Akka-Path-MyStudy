package com.AkkaEssentialsMain.ActorsIntro

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
object ChildActor extends App{
  object Parent{
    case class CreateChildActor(childActor:String)
    case class TellChildActor(msg:String)
  }
  class Parent extends Actor{
    import Parent.*

    override def receive: Receive ={
      case CreateChildActor(name)=>
        println(s"${self.path} creating child")
        //created child actor with context.actorOf referencing  the child class name in the props
        val childRef: ActorRef = context.actorOf(Props[ChildActor](),name)
        context.become(withChild(childRef))
    }
    def withChild(childRef: ActorRef):Receive={
      case TellChildActor(msg) => childRef forward msg
    }
  }
  class ChildActor extends Actor{
    override def receive: Receive = {
      case msg => println(s"${self.path} I Got $msg")
    }
  }
  import Parent.*
  val actorSystem = ActorSystem("ParentChild")
  val parentActorInstance = actorSystem.actorOf(Props[Parent](),"parent")
  parentActorInstance ! CreateChildActor("ChildActor")
  parentActorInstance ! TellChildActor("Hi There")
  //select child by given path and send message to the child
  val childSelection = actorSystem.actorSelection("/user/parent/ChildActor")
  childSelection ! "Found You"
}
