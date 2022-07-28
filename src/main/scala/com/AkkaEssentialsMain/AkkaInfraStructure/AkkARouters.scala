package com.AkkaEssentialsMain.AkkaInfraStructure

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Terminated}
import akka.routing.{ActorRefRoutee, Broadcast, FromConfig, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router}
import com.typesafe.config.ConfigFactory

//We Use Routers When We Want To Delegate Work Between Multiple Actors Of The Same Kind
//Routers Are Middle Level Actors That Forward Messages To other Actors
object AkkARouters extends App {
  //create Manager --> 5 Employees Sub Actors
  //==========>Method One Manual Implementation
  class ManagerActor extends Actor {
    //step one ==> create sub actorRefRoutee(employee) based on Employee Actor
    private val employees: IndexedSeq[ActorRefRoutee] = for (i <- 1 to 5)
      yield{
        val employee: ActorRef = context.actorOf(Props[Employee](),s"Employee_$i")
        context.watch(employee)
        ActorRefRoutee(employee)
      }
    //step two ==> create the router logic
    //Router takes 2 parameters  Router(logic: RoutingLogic, routees: IndexedSeq[Routees])
    private val router = Router(RoundRobinRoutingLogic(), employees)
    //Step 3 ==>route and  handle messages
    //step 4 =>handle the termination cycle of actor as i register myself to death watch
    override def receive: Receive = {
      case Terminated(ref) =>
        //scenario where one of employee actor dies replace it with another one
        //first we remove the routee
        router.removeRoutee(ref)//
        val newEmployee = context.actorOf(Props[Employee]())
        context.watch(newEmployee)
        router.addRoutee(newEmployee)
      case msg =>
        router.route(msg,sender())
    }
  }
  class Employee extends Actor with ActorLogging {
    override def receive: Receive = {
      case msg =>
        log.info(msg.toString)
    }
  }
  val actorSystem = ActorSystem("RouterDemo",ConfigFactory.load().getConfig("routersDemo"))
  val managerActor = actorSystem.actorOf(Props[ManagerActor]())
  for i <- 1 to 10 do managerActor ! s"Hello Employee No: $i"
  //prints go 1->1 2->2 3->3 4->4 5->5 then start from 1->6 2->7 3->8 4->9 5->10
  //[akka://RouterDemo/user/$a/Employee_1] Hello Employee No: 1
  //[akka://RouterDemo/user/$a/Employee_5] Hello Employee No: 5
  //[akka://RouterDemo/user/$a/Employee_3] Hello Employee No: 3
  //[akka://RouterDemo/user/$a/Employee_4] Hello Employee No: 4
  //[akka://RouterDemo/user/$a/Employee_1] Hello Employee No: 6
  //[akka://RouterDemo/user/$a/Employee_5] Hello Employee No: 10
  //[akka://RouterDemo/user/$a/Employee_3] Hello Employee No: 8
  //[akka://RouterDemo/user/$a/Employee_4] Hello Employee No: 9
  //[akka://RouterDemo/user/$a/Employee_2] Hello Employee No: 2
  //[akka://RouterDemo/user/$a/Employee_2] Hello Employee No: 7
  //==========================================================================================
  //Other Available Options For Routing Logic:
  //A)Round Robin -> cycles between routees
  //B)Random -> random delegation between actors
  //C)smallest Mailbox -> As Load Balancing As It Always send Actor With The fewest Message In Queue
  //D)Broadcast -> Send same messages To All Routees
  //F)Scatter-gather-first -> send to all Rotees and all next replies discarded
  //G)Tail Shopping ==>forward the next Message to each Actor Sequentially until the first reply receive and all the others discarded
  //H)consistent Hashing ==> All Messages With The Same Hash Get to the same Actor  and wait for the first reply
  //===========================================================================================
  //========>Method 2 Router Actor With It's Own Children {{Pool Router}}
  //                2.1 ===> Programmatically
  val roundRobinPool = actorSystem.actorOf(RoundRobinPool(5).props(Props[Employee]()),"RoundRobinPool")
  //same thing as above with 2 lines of code
  for i <- 1 to 10 do roundRobinPool ! s"Hello Employee No: $i" //same result as above
  //              2.2 ====> Configuration
  //add follow configuration on application.conf:
  /*routersDemo{
      akka{
      actor.deployment{
      #name of actor going to Instantiate
      /roundRobinPoolMaster2{
      router = round-robin-pool
      nr-of-instances = 5
      }
      }
      }
  }*/
  //then apply ConfigFactory in ActorSystem As Shown Above
  val roundRobinPoolMaster2 = actorSystem.actorOf(FromConfig.props(Props[Employee]()),"roundRobinPoolMaster2")
  for i <- 1 to 10 do roundRobinPoolMaster2 ! s"Hello Employee No: $i"
  //=============================================================================================
  //===>Method 3
  //          3.1 Routers with actors created else where {{Group Route}}
  //in another place in large application these employee actors were created
  val employeeList: List[ActorRef] = (1 to 5).map(i =>actorSystem.actorOf(Props[Employee](),s"Employee_$i")).toList
  //need their path
  val employeePath: List[String] = employeeList.map(employeeRef=>employeeRef.path.toString)

  val groupEmployeeRoute: ActorRef = actorSystem.actorOf(RoundRobinGroup(employeePath).props())
  for i <- 1 to 10 do groupEmployeeRoute ! s"Hello Employee No: $i" //same result as above

  //        3.2 from configuration going to application.conf
  //Whole Route configuration also check it in github
  /*# routers demo
  routersDemo{
      akka{
      actor.deployment{
      #name of actor going to Instantiate
      /roundRobinPoolMaster2{
      router = round-robin-pool
      nr-of-instances = 5
      }
      /groupRoundRobinMaster2{
      router = round-robin-group
      routees.paths = ["/user/Employee_1","/user/Employee_2","/user/Employee_3","/user/Employee_4","/user/Employee_5"]
      }
      }
      }*/
  val groupRoundRobinMaster2 = actorSystem.actorOf(FromConfig.props(),"groupRoundRobinMaster2")
  for i <- 1 to 10 do groupRoundRobinMaster2 ! s"Hello Employee No: $i" //same result as above
  //=================================================================================
  //handling special messages
  groupRoundRobinMaster2 ! Broadcast("Hi All") //this will be send to all actors regardless to their strategy
  // PoisonPill and Kill are NOT routed
  // AddRoutee, Remove, Get handled only by the routing actor
}
