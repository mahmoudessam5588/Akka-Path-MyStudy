package com.AkkaEssentialsMain.AkkaInfraStructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.dispatch.{ControlMessage, PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.{Config, ConfigFactory}

//MailBox control How messages are stored for actors
object AkkaMailBox extends App{
  class MsgActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case msg => log.info(msg.toString)
    }
  }
    //Implement Custom MailBox Which Prioritize the messages it enqueues first
    /*Prioritize Message :
  * P0 -> Most Important
  * P1 ->
  * P2 ->*/
    //=====>Step One Create The Class
    //important parameters:     ===>then extends UnboundedPriorityMailbox(type Comparator[Envelope]Takes partial function)
    //A)ActorSystem.Settings    ===>of PriorityGenerator at run time it's instantiated by reflection
    class SupportTicketPriorityMailBox(settings: ActorSystem.Settings, config: Config)
      extends UnboundedPriorityMailbox(PriorityGenerator {
        case msg: String if msg.startsWith("[P0]") => 0 //lower number higher priority
        case msg: String if msg.startsWith("[P1]") => 1 //lower number higher priority
        case msg: String if msg.startsWith("[P2]") => 2 //lower number higher priority
        case msg: String if msg.startsWith("[P3]") => 3 //lower number higher priority
        case _ => 4
      })
  //====>Step Two Make It Known In Configuration see application.conf configuration
 /*support-dispatcher{
     mailbox-type = "com.AkkaEssentialsMain.AkkaInfraStructure.AkkaMailBox$SupportTicketPriorityMailBox"
 }
 #mailbox
 mailboxesDemo{

 }*/
  //====> Step Three Attach Dispatcher With An Actor
  val actorSystem = ActorSystem("MailBocDemo",ConfigFactory.load().getConfig("MailboxesDemo"))
  val supportTickActor = actorSystem.actorOf(Props[MsgActor]().withDispatcher("support-dispatcher"))
  supportTickActor ! "[P3] Ticket"
  supportTickActor ! "[P0] Ticket"
  supportTickActor ! "[P1] Ticket"
  supportTickActor ! "[P2] Ticket"
  //prints
  /*[akka://default/user/$a] [P0] Ticket
     [akka://default/user/$a] [P1] Ticket
     [akka://default/user/$a] [P2] Ticket
    [akka://default/user/$a] [P3] Ticket*/
  //==================================================================
  //Control Aware MailBox
  //we'll use UnboundedControlAwareMailbox
  //step1 we need to mark a message as being priority message by marking it as control message
  case object TicketManagement extends ControlMessage
  //step2 configure awareness when gets the mailbox ==> makes actor attached to the mailbox no arrangement here
  //see control-mailbox at application.conf
  //method-1
  val controlAwareActor = actorSystem.actorOf(Props[MsgActor]().withMailbox("control-mailbox"))
  controlAwareActor ! "[P3] Ticket"
  controlAwareActor ! "[P0] Ticket"
  controlAwareActor ! "[P1] Ticket"
  controlAwareActor ! "[P2] Ticket"
  controlAwareActor ! TicketManagement
  //prints that actor $b Puts TicketManagement first then the same $b handled  every message as they displayed
  //[akka://MailBocDemo/user/$b] TicketManagement
  //[akka://MailBocDemo/user/$b] [P3] Ticket
  //[akka://MailBocDemo/user/$b] [P0] Ticket
  //[akka://MailBocDemo/user/$b] [P1] Ticket
  //[akka://MailBocDemo/user/$b] [P2] Ticket
  //=========================================================
  //method 2 -using deployment config
  //must ne configured in application.conf
  val altControlAwareActor = actorSystem.actorOf(Props[MsgActor](),"altControlAwareActor")
  altControlAwareActor ! "[P3] Ticket"
  altControlAwareActor ! "[P0] Ticket"
  altControlAwareActor ! "[P1] Ticket"
  altControlAwareActor ! "[P2] Ticket"
  altControlAwareActor ! TicketManagement
  //same behaviour
}
