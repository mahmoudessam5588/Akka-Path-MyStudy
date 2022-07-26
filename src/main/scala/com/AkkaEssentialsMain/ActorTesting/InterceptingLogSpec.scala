package com.AkkaEssentialsMain.ActorTesting

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
class InterceptingLogSpec
  extends TestKit(ActorSystem("InterceptingLogSpec",ConfigFactory.load().getConfig("interceptingLogMessages")))
with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll
{
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  //Intercepting Log Messages Is Very Useful In Integration Testing
  //So What Is Integration Testing??
  //is a type of software testing in which the different units,
  //modules or components of a software application are tested as a combined entity.
  //However,these modules may or may be not  coded by different programmers
  //in the Below Example I Demonstrate An Example Of Hr Department Filtering Resumes Process
  //Where The Master Actor Create Two Children (TalentAcquisition,HrAcknowledgment)
  //to filter Resumes And Return The Computing Result As Displayed Below
  //Here It's Vey Hard To Inject TestProbe For Testing
  //Also If You See The Whole Flow It's Pretty Complex As You HAve A Number Of Actors Interacting With EachOther
  //Also We can't Do Message Base Testing
  //so let's see what we can do
  import InterceptingLogSpec.*
  "A Filtering Checking Resumes Process"should{
    val resume = "Mahmoud Essam Resume"
    val jobPosition = "2 Years Scala Software Engineer"
    val rejectedJobPosition = "0 years Scala Software Engineer"
    "Check Resumes That Proceed  Further" in{
    //we will use EventFilter That Check Last Log Message in HrAcknowledgment is being log
    //that unfortunately what we can do in this type of architecture3
    //we can't Intervene in the actor architecture or either inject test probe into our actor hierarchy
    //but we can look into log messages so lets' do that
    //EventFilter.Info ==> create EventFilter Object Which Scans for logging messages
    //we will pass pattern regex string that match the log
    //s"Application $applicationNumber with Resume $resume can  proceed further"
    //for this pattern to be caught and test succeed we need to intercept{} partial function
    EventFilter.info(pattern=s"Application [0-9]+ for Resume $resume can proceed further")intercept
      {
        //our test code
        val resumeFilteringProcess = system.actorOf(Props[ResumeFilteringProcess]())
        resumeFilteringProcess ! CheckingResumes(resume, jobPosition)
      }
    }
    //here we check the level on logging in Error level throwing Run Time Exception In case of Resume
    //not fit the criteria of years of Experience but as you know the default run time for testing actor is 3 seconds
    //so we have to edit the leeway time of eventFilter and add it in application,.conf and make the test kit recognize it as above
    //here the configuration you can check github for more details:
    /*# intercepting log messages test
    interceptingLogMessages {
      akka {
        loggers = ["akka.testkit.TestEventListener"]
        test {
          filter-leeway = 5s
        }
      }
    }*/
    "Unfortunate Candidates Resume"in{
      EventFilter[RuntimeException](occurrences = 1)intercept{
        val resumeFilteringProcess = system.actorOf(Props[ResumeFilteringProcess]())
        resumeFilteringProcess ! CheckingResumes(resume, rejectedJobPosition)
      }
    }
  }
}
object InterceptingLogSpec {
  case class CheckingResumes(resume: String, jobPosition: String)
  case class EligibleResumes(jobPosition: String)
  case object ResumeAccepted
  case object ResumeRejected
  case class ResumeProceed(resume: String)
  case object ResumeApprovedForInterview
  class ResumeFilteringProcess extends Actor {
    //create two Actor children
    private val talentAcquisition = context.actorOf(Props[TalentAcquisition]())
    private val hrAcknowledgment = context.actorOf(Props[HrAcknowledgment]())
    override def receive: Receive = awaitingResumeComputing
    def awaitingResumeComputing: Receive = {
      case CheckingResumes(resume, jobPosition) =>
        talentAcquisition ! EligibleResumes(jobPosition)
        context.become(pendingResumes(resume))
    }
    def pendingResumes(resume: String): Receive = {
      case ResumeAccepted =>
        hrAcknowledgment ! ResumeProceed(resume)
        context.become(fulfilmentCriteriaResumes(resume))
      case ResumeRejected =>
        throw new RuntimeException("No Luck With That Candidate Unfortunately")
    }
    def fulfilmentCriteriaResumes(str: String):Receive={
      case ResumeApprovedForInterview => context.become(awaitingResumeComputing)
    }
  }
  class TalentAcquisition extends Actor {
    override def receive: Receive = {
      case EligibleResumes(jobPosition) =>
        //simulate any filtering like not accepting refreshers with 0 experience
        if jobPosition.contains("0") then sender() ! ResumeRejected
        else {
          Thread.sleep(4000) //simulate computing and human filtering
          sender() ! ResumeAccepted
        }
    }
  }
  class HrAcknowledgment extends Actor with ActorLogging {
    var applicationNumber = 55
    override def receive: Receive = {
      case ResumeProceed(resume) =>
        applicationNumber += 1
        log.info(s"Application $applicationNumber with Resume $resume can proceed further")
        sender() ! ResumeApprovedForInterview
    }
  }
}
