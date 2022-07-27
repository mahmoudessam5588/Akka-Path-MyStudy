package com.AkkaEssentialsMain.FaultTolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

object StartingStoppingActors extends App {
  val actorSystem = ActorSystem("StoppingActor")
  object Boss {
    case class StartWorker(name: String)
    case class StopWorker(name: String)

    case object Stop
  }
  class Boss extends Actor with ActorLogging {
    import Boss.*
    override def receive: Receive = awaitingWorker(Map())
    def awaitingWorker(worker: Map[String, ActorRef]): Receive = {
      case StartWorker(name) =>
        log.info(s"Starting Child $name")
        context.become(awaitingWorker(worker + (name -> context.actorOf(Props[Worker](), name))))
      case StopWorker(name) =>
        log.info(s"Stopping Worker With Name $name")
        //retrieve the key from worker Map()
        val stopWorker = worker.get(name)
        //iterate the key over the value of workerRef:ActorRef and stop the ActorRef
        //this will lead stopping the worker actor then what remains is the main Boss Actor {self}
        stopWorker.foreach(workerRef => context.stop(workerRef))
      case Stop =>
        //here we stop the {self} Boss main Actor
        log.info("Stopping Myself")
        context.stop(self) //self equivalent to this In OOP
      case message =>
        log.info(message.toString)
    }
  }
  class Worker extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }
  //--------------------------------------------------------------------
  /*####Method 1 ===> Using Context.stop()*/
  import Boss.*
  val bossActor = actorSystem.actorOf(Props[Boss]())
  bossActor ! StartWorker("WorkerOne")
  val workerOne = actorSystem.actorSelection("/user/$a/WorkerOne")
  workerOne ! "Signal Worker One"
  //lets stop worker actors Notice:
  //context.stop() happens asynchronously that doesn't mean worker actors stops right away
  //to prove that lets do an iteration loop
  bossActor ! StopWorker("WorkerOne")
  //for _ <- 1 to 50 do workerOne ! "Have You Stopped?"
  // 5 messages printed to the console before encountering dead letters
  //next lets stop the Main actor Boss Actor
  //this also stop asynchronously but Stop --> stops everything even the worker actors as you know
  //before from child hierarchy
  bossActor ! StartWorker("WorkerTwo")
  val workerTwo = actorSystem.actorSelection("/user/$a/WorkerTwo")
  workerTwo ! "Signal Worker Two"
  //then RIOT Occurred and all left the work for underpayment and Long Working Hours🤨
  //so lets STOP EVERYTHING LET IT STOP
  bossActor ! Stop
  //try to send massages to Boss and workers buts ITS ALL GONE ITS TOO LATE !!!!!
  for _ <- 1 to 10 do bossActor ! "Boss Are you Still There Please Respond Are You There!!!"//boss not responding
  //now we panic and send 100 messages to worker actor!!!!
  for i <- 1 to 100 do workerTwo ! s"Workers No: $i Please Respond Are you Still There!!!"
  //9 worker signal sent  before all stop responding !!!! == dead letters
  // the Stop object stops all the workers first before stopping the boss actor
  /*----------------------------------------------------------------------------------*/
  /*###Method2 Using Special Message Poison Pill*/
  val defiantWorkerActor =actorSystem.actorOf(Props[Worker]())
  defiantWorkerActor ! "You Shall Be Terminated" //!!!!
  defiantWorkerActor ! PoisonPill //please don't judge me it's akka framework naming convention 🥴
  defiantWorkerActor ! "Defiant Actor Are You Still Alive!!!" //🤨
  //message above not delivered instance stop of defiant worker actor
  //-----------------------------------------------------------------------
  /*###Method3 Using Special Message Kill*/
  val anarchistWorkerActor = actorSystem.actorOf(Props[Worker]())
  anarchistWorkerActor ! "YOOOOOUUUU SHHHHHAALLLLL NOOOOTTT PASSSSS!!!!!"
  anarchistWorkerActor ! Kill //again please don't judge me it's akka framework naming convention 🥴
  //kill message a little more brutal than PoisonPill
  anarchistWorkerActor ! "RUUUN YOU FOOOOOOLS"//🤨 !!!
  //Kill Message appears on console logs as an Error
  //[ERROR] [07/27/2022 00:11:39.443] [StoppingActor-akka.actor.internal-dispatcher-3] [akka://StoppingActor/user/$b] Kill (akka.actor.ActorKilledException: Kill)
  //both special messages handled separately by actors
  //---------------------------------------------------------------------
  /*####Method4 DEATH WATCH*/
  //it's mechanism to be notified when an actor dies
  //for last time don't judge me 🤨
  class DeathWatcher extends Actor with ActorLogging{
    import Boss.*
    override def receive: Receive = {
      case StartWorker(name)=>
        val worker = context.actorOf(Props[Worker](),name)
        log.info(s"Starting Watching Worker $name")
        context.watch(worker) //you are about to be Terminated ~!!!!
      case Terminated(ref)=>//special case provided by akka when watch is invoked
      log.info(s"the ref worker That I'm Watching $ref Have been Terminated")
    }
  }
  val theWatcherActor = actorSystem.actorOf(Props[DeathWatcher](),"watcher")
  theWatcherActor ! StartWorker("watchWorker")
  val watchedWorker = actorSystem.actorSelection("/user/watcher/watchedChild")
  Thread.sleep(5000)
  watchedWorker ! PoisonPill
}
