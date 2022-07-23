package com.AkkaEssentialsMain.ActorsIntro

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorExample extends App {
  //Imagine If We Have A Huge book and we want to search and count certain letter or name in the book
  //or maybe replace certain letter to symbol or upperCase
  //in searching algorithm like Naive search or Robin-Karp search
  //will not help us that much in average scenario and worst case scenario
  //so what if we take a whole different approach and embrace Akka parallelism to maximum extent
  //we will create Actor hierarchy or one Supervised Master actor
  // and Dedicated workers that distributed the given tasks and return the result
  //any hard computational task can take significant time and can be easily parallelizable
  //for Supervising or Scheduling Dedicated Workers  we will use Round Robin Logic
  //So What is ROUND Robin Logic??
  //is an arrangement of choosing all elements in a group equally in some rational order,
  //usually from the top to the bottom of a list and then starting again at the top of the list
  // and so on. A simple way to think of round robin is that it is about "taking turns."
  object SupervisedMasterActor {
    case class InstantiateWorkerActors(nWorkers: Int)
    case class CharCountTask(id: Int, text: String)
    case class CharCountReply(id: Int, count: Int)
  }
  class SupervisedMasterActor extends Actor {

    import SupervisedMasterActor.*
    override def receive: Receive = {
      case InstantiateWorkerActors(nWorkers) =>
        println("[Supervised Master Actor] Instantiation.....")
        //instantiate number of actor workers according to given nWorker
        val workersRef = for i <- 1 to nWorkers yield context.actorOf(Props[DedicatedWorkerActor](), s"Worker_NO:$i")
        //change in mutable stable need  content.become with workerWorking method to scheduling task
        context.become(workerWorking(workersRef, 0, 0, Map()))
    }
    //
    def workerWorking(workersRef: Seq[ActorRef], currentWorkerIndex: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]):Receive={
      //receiving text and scheduling text to workers
      case text:String =>
        println(s"[Supervised Master Actor]Received The $text - I will send it to Worker $currentWorkerIndex")
        //send message back to master assigned sender to val we will use it later in new RequestMap & CharCountReply
        val originalSender: ActorRef = sender()
        //start working on the task
        //we need Ids in task and worker Id for round robin logic to see who's turn is it and schedule them correctly
        val workerTask: CharCountTask = CharCountTask(currentTaskId,text)
        //selecting the workerRef at Index currentWorkerIndex which equals to zero
        val workerRef: ActorRef = workersRef(currentWorkerIndex)
        //send a workerRef a new task to start working on it
        workerRef ! workerTask
        //we need to increment next worker index and mod operator makes sure that thr next index go to zero
        //if goes beyond WorkerRef limit
        val nextWorkerIndex = (currentWorkerIndex+1) % workersRef.length
        //Increment Task Id By 1
        val newTaskId = currentTaskId +1
        //we use map to know with taskId with Which original Sender
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(workerWorking(workersRef,nextWorkerIndex,newTaskId,newRequestMap))
      case CharCountReply(id , count) =>
        println(s"[Supervised Master Actor] I Received a Reply for Task id $id with $count")
        //retrieve the original sender from request map
        val originalSender = requestMap(id)
        originalSender ! count
        //change in immutable state occurred so context.become
        //we keep all the same values but remove Id from requestMap
        context.become(workerWorking(workersRef, currentWorkerIndex, currentTaskId, requestMap-id))
    }
  }
  class DedicatedWorkerActor extends Actor {
    import SupervisedMasterActor.*
    override def receive: Receive =
      //receiving the task to process
      case CharCountTask(id ,text)=>
        println(s"${self.path} I Have received task $id with $text")
        //implement any logic you need here
        sender() ! CharCountReply(id,text.count(_.==('o')))

    }
  class InvokingProcess extends Actor{
    import SupervisedMasterActor.*

    override def receive: Receive = {
      case "Invoke" =>
        val superVisedMasterActor = context.actorOf(Props[SupervisedMasterActor](),"Master")
        superVisedMasterActor ! InstantiateWorkerActors(4)
        val texts = List("Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?",
          "At vero eos et accusamus et iusto odio dignissimos ducimus qui blanditiis praesentium voluptatum deleniti atque corrupti quos dolores et quas molestias excepturi sint occaecati cupiditate non provident, similique sunt in culpa qui officia deserunt mollitia animi, id est laborum et dolorum fuga. Et harum quidem rerum facilis est et expedita distinctio. Nam libero tempore, cum soluta nobis est eligendi optio cumque nihil impedit quo minus id quod maxime placeat facere possimus, omnis voluptas assumenda est, omnis dolor repellendus. Temporibus autem quibusdam et aut officiis debitis aut rerum necessitatibus saepe eveniet ut et voluptates repudiandae sint et molestiae non recusandae. Itaque earum rerum hic tenetur a sapiente delectus, ut aut reiciendis voluptatibus maiores alias consequatur aut perferendis doloribus asperiores repellat."
        ,"On the other hand, we denounce with righteous indignation and dislike men who are so beguiled and demoralized by the charms of pleasure of the moment, so blinded by desire, that they cannot foresee the pain and trouble that are bound to ensue; and equal blame belongs to those who fail in their duty through weakness of will, which is the same as saying through shrinking from toil and pain. These cases are perfectly simple and easy to distinguish. In a free hour, when our power of choice is untrammelled and when nothing prevents our being able to do what we like best, every pleasure is to be welcomed and every pain avoided. But in certain circumstances and owing to the claims of duty or the obligations of business it will frequently occur that pleasures have to be repudiated and annoyances accepted. The wise man therefore always holds in these matters to this principle of selection: he rejects pleasures to secure other greater pleasures, or else he endures pains to avoid worse pains."
        )
        texts.foreach(text=>superVisedMasterActor ! text)
      case count:Int =>
        println(s"[Invoking Actors]recieved replay $count")
    }
  }
  val actorSystem = ActorSystem()
  val invokedActors = actorSystem.actorOf(Props[InvokingProcess](),"invokedActors")
  invokedActors ! "Invoke"
  }



