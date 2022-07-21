package com.AkkaEssentialsMain.ActorsIntro

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
object MutableStateActorExercises extends App {
  //small motel room rental example
  object RoomRental {
    case class CheckedIn(lengthOfStay: Int)
    case class CheckingOut(finishedStayLength: Int)
    case class RoomIsOccupied(msg: String)
    case class RoomISEmpty(msg: String)
  }

  class RoomRental extends Actor {
    var num0fDays = 0 // immutable not best practise and not recommended
    //we will discuss later immutable change of state by using context.become
    import RoomRental.*
    override def receive: Receive = {
      //it's a busy motel so lets assume that maximum length of stay is 7 days
      // for demonstration purposes
      case CheckedIn(lengthOfStay) =>
        if lengthOfStay >= 7 then sender() ! RoomISEmpty("Exceed length of stay paid extra pill & We Have Room Available")
        else {
          num0fDays += lengthOfStay
          sender() ! RoomIsOccupied(s"Unfortunately Room Is Busy , daysPassedToLeave = $lengthOfStay")
        }
      case CheckingOut(finishedStayLength) =>
        if (finishedStayLength >= 7) {
          sender() ! RoomISEmpty("You should Leave You can only stay for 7 days")
        }
        else {
          num0fDays -= finishedStayLength
          sender() ! RoomIsOccupied(s"Days Left To Check Out $finishedStayLength")
        }
    }
  }

  object Customer {
    case class CheckingTheMotel(stay: ActorRef)
  }

  class Customer extends Actor {

    import Customer.*
    import RoomRental.*

    override def receive: Receive = {
      case CheckingTheMotel(stay) =>
        stay ! CheckedIn(5) //customer checked in for 5 days
        stay ! CheckingOut(3) //customer is checking out after 3 days
        stay ! CheckedIn(8) //customer exceed his length of stay and paid extra pill is checkout now
        stay ! CheckingOut(7) //informing a customer that is his last stay of stay before checking out
      case msg => println(msg.toString)
    }
  }
  val actorSystem = ActorSystem()
  val room = actorSystem.actorOf(Props[RoomRental](), "rental")
  val customer = actorSystem.actorOf(Props[Customer](), "Customer")
  import Customer.*
  customer ! CheckingTheMotel(room)
}
