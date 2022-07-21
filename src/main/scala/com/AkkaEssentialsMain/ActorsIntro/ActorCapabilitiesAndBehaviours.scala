package com.AkkaEssentialsMain.ActorsIntro

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.AkkaEssentialsMain.ActorsIntro.ActorCapabilitiesAndBehaviours.MatchingPatterns.{OrderedToIntroduceMySelf, OrderedToSendGiftToTargetActorRef, SignalMessageReceiver, TargetActorRef}

object ActorCapabilitiesAndBehaviours extends App {
  object MatchingPatterns {
    case class SignalMessageReceiver(contents: String)

    case class OrderedToIntroduceMySelf(contents: String)

    case class TargetActorRef(ref: ActorRef)

    //we added actor ref for receive massage to direct message to actor reference
    case class OrderedToSendGiftToTargetActorRef(contents: String, ref: ActorRef)
  }

  class MatchingPatterns extends Actor {
    override def receive: Receive = {
      //normal matching
      case "Greeting" =>
        println("Matched the String Object greeting My Position Is Curial Because If I Was To Be  Placed below strMatcher I will Never Be Called And Displayed Take Care of My Placement")
      case strMatcher: String =>
        println(s"I Display Result Of  Any String Or Case Classes Have STRING MEMBERS= $strMatcher")
      //the code below enters infinite loop but was only showcasing that you can replay by message to Actors
      //context.sender() ! "replay Back By Saying I'M Grateful For The Gift I Received From You"
      case intMatcher: Int => println(s"Age = $intMatcher")
        // behaviour change according to logic passed
        import MatchingPatterns.*
      case SignalMessageReceiver(msg) => println(s"Receive Signal From Actor Directing My $msg")
      case OrderedToIntroduceMySelf(msg) => self ! msg
      case TargetActorRef(ref) => ref ! "Gift From Actor"
      case OrderedToSendGiftToTargetActorRef(content, ref) =>
        ref ! (content + "s") //equivalent to  (ref ! (content + "s")(self)
      case _ =>
    }
  }

  val actorSystem = ActorSystem("TestingBehaviour")
  val actorInstance = actorSystem.actorOf(Props[MatchingPatterns](), "Actors")
  //testing matching pattern Matching
  actorInstance ! "Mahmoud Essam"
  actorInstance ! 34
  actorInstance ! "Greeting"


  import MatchingPatterns.*

  actorInstance ! SignalMessageReceiver("Message I Received Back To Him")
  actorInstance ! OrderedToIntroduceMySelf("Hi This Message Has Been Signal From Actor to Introduce MySelf By Receive Method")
  //actors reply to massages and send messages o one another
  val targetActorRef = actorSystem.actorOf(Props[MatchingPatterns](), "TargetActorRef")
  val theDestinationTarget = actorSystem.actorOf(Props[MatchingPatterns](), "OrderedToSendGiftTo")
  actorInstance ! TargetActorRef(theDestinationTarget)
  actorInstance ! OrderedToSendGiftToTargetActorRef("s", targetActorRef)
  /*prints the following:
  I Display Result Of  Any String Or Case Classes Have STRING MEMBERS= Mahmoud Essam
  --------------------------------------------------------------
  Age = 34
  ---------------------------------------------------------------
  Matched the String Object greeting My Position Is Curial Because
  If I Was To Be  Placed below strMatcher
   I will Never Be Called And Displayed Take Care of My Placement
  -------------------------------------------------------------
Receive Signal From Actor Directing My Message I Received Back To Him
  ----------------------------------------------------------------------
I Display Result Of  Any String Or Case Classes Have STRING MEMBERS= Gift From Actor
  ----------------------------------------------------------------------------
I Display Result Of  Any String Or Case Classes Have STRING MEMBERS= Hi This Message Has Been Signal From Actor
to Introduce Myself By Receive Method
  -------------------------------------------------------------------------------
I Display Result Of  Any String Or Case Classes Have STRING MEMBERS= ss */
  //Important note to ! (tell Method) There is the message signature
  //def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit
  //every message take Any and have implicit value of actor ref and return a Unit
}
