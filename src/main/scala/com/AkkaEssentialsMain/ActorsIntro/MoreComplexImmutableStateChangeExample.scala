package com.AkkaEssentialsMain.ActorsIntro

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object MoreComplexImmutableStateChangeExample extends App {

  //applying for online exam(S) example
  object ExamCandidate {
    //below CandidateExam of course instead of String you can Use Enums but I'm using String Here For simplification
    case class CandidateApplied(candidateExam: String)

    case object CandidateApplicationRequest

    case class CandidateApplicationReplay(candidate: Option[String])
  }

  //Exam Candidate Request  and waiting replay behaviour either applied or Not Yet
  class ExamCandidate extends Actor {

    import ExamCandidate.*

    override def receive: Receive = {
      case CandidateApplied(candidateExam) => context.become(applied(candidateExam))
      case CandidateApplicationRequest => sender() ! CandidateApplicationReplay(None)
    }

    def applied(candidate: String): Receive = {
      case CandidateApplicationRequest => sender() ! CandidateApplicationReplay(Some(candidate))
    }
  }

  //next step collect application for candidates who applied online
  object AllApplications {
    //collect all candidates and show who have Applied Or Not
    //set to prevent duplicate entries
    //Actor Ref Will be evaluated by Instance Of Actor Instantiation
    case class ApplicationCollection(candidates: Set[ActorRef])
  }

  class AllApplications extends Actor {

    import ExamCandidate.*
    import AllApplications.*

    override def receive: Receive = awaitingApplicationRequests

    def awaitingApplicationRequests: Receive = {
      //iterate on each candidate of candidate and collect candidate Application requests
      case ApplicationCollection(candidates) =>
        candidates.foreach(candidateRef => candidateRef ! CandidateApplicationRequest)
        //this lead to change of behaviour state  so we need
        //1)context.become
        //2) method to collect applications and replay not candidates that haven't applied yet
        context.become(applicationsStatus(candidates, Map()))
    }

    def applicationsStatus(waitingToApply: Set[ActorRef], currentStatus: Map[String, Int]): Receive = {
      //1)candidates haven't applied yet
      case CandidateApplicationReplay(None) =>
        sender() ! CandidateApplicationRequest
      //2)candidates who have applied
      case CandidateApplicationReplay(Some(candidate)) =>
        val newWaitingToApply: Set[ActorRef] = waitingToApply - sender() // remove sender from actor ref to map it later
        val currentAppliedCandidates: Int = currentStatus.getOrElse(candidate, 0) //Map(K,V) pairs extract value of Int
        //complex logic below lets demystify it piece by piece
        //simply we adding two maps to each other of type Map[String,Int]
        //Map on the Left is The Current Candidate How Applied For Exams
        //Map on the right is (candidate[key of String] -> currentAppliedCandidate[Value Of Int + 1])
        val newCandidateStatus: Map[String, Int] =
        currentStatus + (candidate -> (currentAppliedCandidates + 1))
        //condition logic must be applied below to exclude candidates haven't applied yet
        //and print result to the console of type of the exam to applied candidates
        if newWaitingToApply.isEmpty then println(s"[Applications]Status :$newCandidateStatus")
        else context.become(applicationsStatus(newWaitingToApply, newCandidateStatus))
    }
  }

  import ExamCandidate.*
  import AllApplications.*

  val actorSystem = ActorSystem()
  val candidateID45645 = actorSystem.actorOf(Props[ExamCandidate]())
  val candidateID02455 = actorSystem.actorOf(Props[ExamCandidate]())
  val candidateID36987 = actorSystem.actorOf(Props[ExamCandidate]())
  val candidateID45582 = actorSystem.actorOf(Props[ExamCandidate]())
  val candidateID89897 = actorSystem.actorOf(Props[ExamCandidate]())
  //below CandidateExam of course instead of String you can Use Enums but I'm using String Here For simplification
  candidateID02455 ! CandidateApplied("DataBase Engineering Exam")
  candidateID45645 ! CandidateApplied("FrontEnd Development Exam")
  candidateID36987 ! CandidateApplied("Software Architecture Exam")
  candidateID45582 ! CandidateApplied("Software Architecture Exam")
  candidateID89897 ! CandidateApplied("DataBase Engineering Exam")

  val examApplications = actorSystem.actorOf(Props[AllApplications]())
  examApplications ! ApplicationCollection(Set(candidateID02455, candidateID45645, candidateID36987, candidateID45582, candidateID89897))
  //prints:
  //[Application]Status :Map(Software Architecture Exam -> 2, FrontEnd Development Exam -> 1, DataBase Engineering Exam -> 2)
}
