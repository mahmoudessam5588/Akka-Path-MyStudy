package com.AkkaEssentialsMain.ActorsIntro

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
//We want to change Actor Behavior With Time In Immutable Way
//the code below is intuitive mimic the normal human behaviour in eid situation
//read the code as if you are observing different scenarios & Behaviours  with different answers
object EidMoneyOrNoMoneyGift extends App{
  object Child{
    //child normal behaviour and interaction
    case class VisitingRelatives(relativeRef:ActorRef)
    case class AskForMoneyGift(giftPlease:String)
    case class RelativeAsk(msg:String)
    val Delighted = "Received Money Gift"
    val Grief = "Haven't Receive AnyThing"
  }
  object Relative{
    //relative normal behaviour or interaction
    val GiftMoney = "GiftMoney"
    val IgnoredGift = "IgnoredGift"
    object KidRejoiced
    object KidMourns
  }
  class Child extends Actor{
    import Child.*
    import Relative.*
    override def receive: Receive = {
      //refreshing actor reference of relative class below to be evaluated upon
      case VisitingRelatives(relativeRef) =>
        //matched string Received Money Gift
        // after evaluating same method in class  relative below
        relativeRef ! AskForMoneyGift(Delighted)
        relativeRef ! AskForMoneyGift(Grief)
        //haven't Receive AnyThing
        // after evaluating same method in class  relative below
        relativeRef ! AskForMoneyGift(Grief)
        relativeRef ! AskForMoneyGift(Delighted)
        relativeRef ! AskForMoneyGift(Delighted)
        relativeRef ! AskForMoneyGift(Delighted)
        relativeRef ! AskForMoneyGift(Grief)
        relativeRef ! RelativeAsk("Do You Want To Play?")
      //waiting to be matched with relative class below after Ask Invoke signal on case object
      case KidRejoiced => println("My Relative Child Is Happy")
      case KidMourns => println("My Relative Child Is Sad I Wonder Why???!!!!")
    }
  }
  class Relative extends Actor {
    import Child.*
    import Relative.*
    override def receive: Receive = happyKidBehaviour //default state
    def happyKidBehaviour: Receive = {
      case AskForMoneyGift(Delighted) =>
      case RelativeAsk(_) => sender() ! KidRejoiced
      case AskForMoneyGift(Grief) => context.become(sadKidBehaviour,false)
        /*Changes the Actor's behavior to become the new 'Receive' (PartialFunction[Any, Unit])
         handler. This method acts upon the behavior stack as follows:
        if discardOld = true it will replace the top element (i.e. the current behavior)
        if discardOld = false it will keep the current behavior and push the given one atop
        The default of replacing the current behavior on the stack has been chosen
         to avoid memory leaks in case client code is written
         without consulting this documentation first
         (i.e. always pushing new behaviors and never issuing an unbecome())
        use false with context.unbecome if you want to check the percentage or the rate of
        change of behaviour
        use the default without unbecome if only want to read the last code argument and want
        to override the previous ones
        */
    }
    def sadKidBehaviour: Receive = {
      case AskForMoneyGift(Grief) => context.become(sadKidBehaviour,false)
        //Reverts the Actor behavior to the previous one on the behavior stack.
      case AskForMoneyGift(Delighted) => context.unbecome()
      case RelativeAsk(_) => sender() ! KidMourns
    }
  }
  import Child.*
  import Relative.*
  val actorSystem = ActorSystem("EidGift")
  val childInstance = actorSystem.actorOf(Props[Child](),"ChildInstance")
  val relativeInstance = actorSystem.actorOf(Props[Relative](),"RelativeInstance")
  childInstance ! VisitingRelatives(relativeInstance)
}
