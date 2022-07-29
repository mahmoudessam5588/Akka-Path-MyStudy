package com.AkkaEssentialsMain.AkkaPatterns

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import com.AkkaEssentialsMain.AkkaPatterns.AskPatternSpec.{AuthManagement, PipedAuthenticationManager}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.*
import scala.language.postfixOps
import scala.util.{Failure, Success}

//we use Ask Pattern When We Expect Communication With Actors
class AskPatternSpec extends TestKit(ActorSystem("AskPatternSpec"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
  //extract Tests to Prevent Violation Of DRY AND KISS SOLID Principles
  def authenticatorTests(props:Props): Unit ={
    import AskPatternSpec.*
    "An Authenticator" should {
      import AuthManagement.*
      "Failed To Authenticate In Case Of Non Registered User" in {
        val authManager = system.actorOf(props)
        authManager ! Authenticate("mahmoud", "scala")
        expectMsg(AuthenticationFailure(AUTH_FAILURE_NOT_FOUND))
      }
      "Failed To Authenticate In Case Of Invalid Password" in {
        val authManager = system.actorOf(props)
        authManager ! RegistrationUser("mahmoud", "scala")
        authManager ! Authenticate("mahmoud", "scalaz")
        expectMsg(AuthenticationFailure(AUTH_FAILURE_PASSWORD_INCORRECT))
      }
      "Successful To Authenticate In Case Of Register User" in {
        val authManager = system.actorOf(props)
        authManager ! RegistrationUser("ahmed", "123")
        authManager ! Authenticate("ahmed", "123")
        expectMsg(AuthenticationSuccess)
      }
    }
  }
  //Invoke def authenticator tests According to classes here
  "An Authenticator" should {
    authenticatorTests(Props[AuthManagement]())
  }
  "A Piped Authenticator" should {
    authenticatorTests(Props[PipedAuthenticationManager]())
  }
}
object AskPatternSpec {
  //Use Case Of User Authentication
  //We Have User Authentication Manager Actor Who Handles User Authentication.
  case class Read(key: String)
  case class Write(key: String, value: String)
  class KeyValueStoreActor extends Actor with ActorLogging {
    override def receive: Receive = online(Map())
    def online(kv: Map[String, String]): Receive = {
      case Read(key) =>
        log.info(s"Trying To Read A Value At Key: $key")
        sender() ! kv.get(key) //retrieve value associated with key as Option[String]
      case Write(key, value) =>
        log.info(s"writing the value:$value for Key:$key")
        context.become(online(kv + (key -> value)))
    }
  }
  //user authentication actor for  KeyValueStore Actor someone else wrote
  case class RegistrationUser(userName: String, password: String)
  case class Authenticate(UserName: String, password: String)
  case class AuthenticationFailure(msg: String)
  case object AuthenticationSuccess
  case object AuthManagement {
    val AUTH_FAILURE_NOT_FOUND = "username not found"
    val AUTH_FAILURE_PASSWORD_INCORRECT = "password incorrect"
    val AUTH_FAILURE_SYSTEM = "system error"
  }
  class AuthManagement extends Actor with ActorLogging {
    import AuthManagement.*
    implicit val timeout: Timeout = Timeout(1 second)
    implicit val executionContext: ExecutionContext = context.dispatcher
    protected val authKeyValueStore: ActorRef = context.actorOf(Props[KeyValueStoreActor](), "KeyValueStore")
    override def receive: Receive = {
      case RegistrationUser(userName, password) => authKeyValueStore ! Write(userName, password)
      case Authenticate(username, password) => authenticationHandler(username, password)
    }
    def authenticationHandler(username: String, password: String): Unit = {
      val originalSender: ActorRef = sender()
      //in case of authentication we want get the information from KeyValueStoreActor
      //Instead of Sending Username and waiting for password will end up be complicated and slow
      //Akka Provided now way communicating with actors using (?) method imported by
      //akka.pattern.ask
      //this ask(?) method returns a future as potential response I might get from this actor
      //we need 2 Implicit values {TimeOut and executionContext}
      //the below setup breaks Akka Encapsulation as:
      //A)val futureAuth = authKeyValueStore ? Read(username) --->run on a thread
      //b) futureAuth.onComplete ---->runs on a separate thread
      val futureAuth = authKeyValueStore ? Read(username)
      futureAuth.onComplete {
        //NEVER IN YOUR LIFE CALL METHOD{{sender()}}} ON ACTOR INSTANCE {{futureAuth of authKeyValueStore}} OR ACCESS ON MUTABLE STATE
        //this leads to definition called closing over the actor instance or the mutable state
        //solved by val originalSender: ActorRef = sender()
        case Success(None) => originalSender ! AuthenticationFailure(AUTH_FAILURE_NOT_FOUND)
        case Success(Some(storedPassword)) =>
          if storedPassword == password then originalSender ! AuthenticationSuccess
          else originalSender ! AuthenticationFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        case Failure(_) => originalSender ! AuthenticationFailure(AUTH_FAILURE_SYSTEM)
      }
    }
  }
    //strengthen the type better handling of the future preferred as a best practice
    class PipedAuthenticationManager extends AuthManagement {
      import AuthManagement.*
      override def authenticationHandler(username: String, password: String): Unit = {
        val futureAuth = authKeyValueStore ? Read(username)
        val futurePassword = futureAuth.mapTo[Option[String]] //Future[Option[String]]
        val futureResponse = futurePassword.map {
          case None => AuthenticationFailure(AUTH_FAILURE_NOT_FOUND)
          case Some(storedPassword) =>
            if storedPassword == password then AuthenticationSuccess
            else AuthenticationFailure(AUTH_FAILURE_PASSWORD_INCORRECT)
        } // Future[Any] - will be completed with the response I will send back
        // - pipe the resulting future to the actor you want to send the result to
        /*by importing import akka.pattern.pipe
          When the future completes, send the response to the actor ref in the arg list.
         */
        futureResponse.pipeTo(sender())
      }
    }
}
