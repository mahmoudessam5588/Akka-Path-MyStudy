package com.AkkaEssentialsMain.AkkaPatterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}
import com.AkkaEssentialsMain.ActorLoggingAdConfiguration.ActorLoggingIntro.AkkaActorLogging

//Stashes Pattern Let As Put Actor Aside For Later as they can't or they shouldn't process at this
//moment of exact time
//when the time is right prepend them to the mailbox to process them by context.become()
object AKKAStashesPattern extends App {
  //General Concept
  //resources are block for actor
  //if resourceActor Opened
  // - it receives read / write request to the resources
  //- otherwise it will postpone all read/write request until the state is open
  //-----------------------------------------------------------------------------------
  //PseudoCode:
  //ResourceActor
  //   -open => it can receive read/write request to the resources
  //   -otherwise it will  postpone all read/write request until state is open
  //ResourceActor is closed
  //  -open=> Switch to open state
  //   -read /write Messages is POSTPONED
  //resourceActor is open
  //      -read/write are handled
  //        -close => switch to closed state
  //-----------------------------------------------------------------------------------
  //Applying Different Scenarios:
  //[Open,Read ,Read,Write]
  //-switch to open state
  //-read the data
  //-read the data again
  //-write the data
  //[Read,Open,Write]
  //read and write POSTPONED so we will put them in {{{stash: [Read]}}}
  //open=>switch to open state
  //Mailbox: [Read,Write]
  //read and write are handled

  case object Open

  case object Close

  case object Read

  case class Write(data: String)

  //Step One Mixin Stash Trait
  class ResourceActor extends Actor with ActorLogging with Stash {
    //simulate writing to database
    private var innerData: String = ""

    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening Resource")
        //Step 3 un-stashAll when you switch the msg handler
        unstashAll() //prepending all messages to normal mailbox
        context.become(opened)
      case msg =>
        log.info(s"Stashing $msg Postponed till Resource Is Opened")
        //Step 2 Stash Message U can't Handled
        stash()
    }

    def opened: Receive = {
      case Read =>
        //do some actual computation
        log.info(s"I Have Read $innerData")
      case Write(data) =>
        log.info(s"I'm Writing $data")
        innerData = data
      case Close =>
        log.info("Closing Resource")
        unstashAll()
        context.become(closed)
      case msg =>
        log.info(s"Stashing $msg Postponed Can't Handle it In Open State")
        stash()
    }
  }

  val actorSystem = ActorSystem("StashDemo")
  val resourceActor = actorSystem.actorOf(Props[ResourceActor](), "ResourceActor")
  /*resourceActor ! Write("I Love Akka")
  resourceActor ! Read
  resourceActor ! Open*/
  //prints
  //[akka://StashDemo/user/ResourceActor] Stashing Write(I Love Akka) Postponed till Resource Is Opened
  //[akka://StashDemo/user/ResourceActor] Stashing Read Postponed till Resource Is Opened
  //[akka://StashDemo/user/ResourceActor] Opening Resource
  //[akka://StashDemo/user/ResourceActor] I'm Writing I Love Akka
  //[akka://StashDemo/user/ResourceActor] I Have Read I Love Akka
  //Scenario 2
  resourceActor ! Read //stashed
  resourceActor ! Open //switch to open state I have Read ""
  resourceActor ! Open //deal with it as an object and stash it
  resourceActor ! Write("I Love Akka") //I'm Writing I Love Akka
  resourceActor ! Close //switched to closed ->then Open un-stashed Switched To Open !!!!
  resourceActor ! Read // I Have Read
  //prints
  //[akka://StashDemo/user/ResourceActor] Stashing Read Postponed till Resource Is Opened
  //[akka://StashDemo/user/ResourceActor] Opening Resource
  //[akka://StashDemo/user/ResourceActor] I Have Read
  //[akka://StashDemo/user/ResourceActor] Stashing Open Postponed Can't Handle it In Open State {{{Last case in opened msg}}}
  //[akka://StashDemo/user/ResourceActor] Closing Resource {{Un-stashingAll (Open) before closing}}} leads to Open Resource
  //[akka://StashDemo/user/ResourceActor] Opening Resource
  //[akka://StashDemo/user/ResourceActor] I Have Read I Love Akka
}
