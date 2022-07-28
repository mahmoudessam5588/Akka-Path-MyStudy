package com.AkkaEssentialsMain.AkkaInfraStructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

//Dispatchers are in Charge of delivering And Handling Messages within the actor system
object AkkaDispatchers extends App {
  var counter = 0

  class Counter extends Actor with ActorLogging {
    override def receive: Receive = {
      case msg =>
        counter += 1
        log.info(s"Random Msg No:$msg & Counter Increment:$counter")
    }
  }

  val actorSystem = ActorSystem("DispatcherDemo") //,ConfigFactory.load().getConfig("dispatcherDemo")
  //attach custom dispatcher configuration with counterActors as shown below
  //application.conf my-dispatcher configuration (Custom dispatcher)
  /*my-dispatcher{
      type = Dispatcher  ==> #other types will be discussed Below
      executor = "thread-pool-executor"  ==> #Jvm Thread Handler
      thread-pool-executor{
      fixed-pool-size = 3    ==>#how many threads to allocate and schedule at any one time
          ==> if we fixed pool size to one means one asynchronous single threaded non blocking that the power of AKkA!!!
      }
      throughput = 30    ===># maximum number of messages dispatcher can handle for one Actor
  }*/
  //Method One ====> Programmatically
  //create more than one actor to see how dispatcher performs
  val counterActors = for i <- 1 to 10 yield actorSystem.actorOf(Props[Counter]().withDispatcher("my-dispatcher"), s"counter_$i")
  //send messages to test the actor
  val random = new Random()
  for i <- 1 to 10000 do counterActors(random.nextInt(10)) ! i
  //each counter processed (n) of times and received exactly 30 msg
  //=========================================================================================================
  //Method Two ======> from configuration
  /*#dispatcherPath
  dispatcherDemo{
      akka.actor.deployment{
      /dispatcherPath{
      dispatcher = my-dispatcher #create a path and attach custom dispatcher to it
      }
      }
  }*/
  val dispatcherPathConfig = actorSystem.actorOf(Props[Counter](), "dispatcherPath")

  //========================================
  //Dispatcher Implement The ExecutionContext trait
  //check scheduling post and how we import system.dispatcher as implicit to implement schedulers to do their work
  //sp we will run implement an actor that run future inside and receives a message
  //in practice we use this example with actor interacting wit some blocking IO or resource like Databases
  class DatabaseActor extends Actor with ActorLogging {
    implicit val executionContext:ExecutionContext = context.system.dispatchers.lookup("my-dispatcher")
    override def receive: Receive = {
      case msg => Future {//future need implicit execution context dispatcher as provided above with our custom dispatcher
        //simulate long computation
        Thread.sleep(4000)
        log.info(s"Done Computation.....$msg")
      }
    }
  }
  val databaseActor = actorSystem.actorOf(Props[DatabaseActor]())
  databaseActor ! "Mahmoud Essam Scala Software Engineer"
  //prints
  //[akka://DispatcherDemo/user/$a] Done Computation.....Mahmoud Essam Scala Software Engineer
  //============================================================================================================
  //Now Lets Test The Non Blocking Logic with both database and counter classes
  val nonBlockingCounterActor = actorSystem.actorOf(Props[Counter]())

  for i <- 1 to 10000  yield {
    val msg = s"Non Blocking $i"
    databaseActor ! msg
    nonBlockingCounterActor ! msg
  }
  //counter work as expected
  //and database actor prints
  // Done Computation.....Non Blocking 33 every 4 seconds
  //this is starvation scenarios where there is hiccups messages printed every 4 seconds blocking over actors to
  //be printed out this scenario sometimes needed in certain circumstances when working with databases
  //like for example if database goes down we need to backoff actors for given amount of time
  //there is a better implementation provided by akka called backoff strategy pattern check my github and previous posts
  //Also best practice to use dedicated custom dispatcher other than the main akka dispatcher as shown above
  //###Solution 2
  //in application.conf
  //type =Dispatcher there are other type of Dispatcher like
  // PinnedDispatcher --->which binned each actor to a thread pool of exactly one thread and thread circle around
  //CallingThreadDispatcher -->Check akka testing ensure that all communication with an actor happen on the calling thread
  //whatever that thread is
}
