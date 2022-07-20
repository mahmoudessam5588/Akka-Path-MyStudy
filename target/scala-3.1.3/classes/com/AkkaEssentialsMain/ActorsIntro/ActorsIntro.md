# Akka Actors Intro
**In build.sbt file include the following**
```sbt
ThisBuild / scalaVersion := "3.1.3"
val scalaTestVersion = "3.2.12"
val akkaVersion = "2.6.19"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion,
)
lazy val root = (project in file("."))
  .settings(
    name := "AKKAMyStudyPath"
  )
```
## Actors Vs Traditional Objects

- In traditional way we model our code through **Instances** of classes
  - Instances have its own data and interact with the world through **Methods**
- In Actors, they also have data but Interaction with the world is Different
    - they way we communicate with Actors, by **Sending Messages To Them** asynchronously
    - **_Actors are objects you can't access directly, but only send messages to._**
  
## Scaling hardship and challenges

- **`Scalability` is the measure to which a system can adapt to a change in demand for
  resources, without negatively impacting performance. `Concurrency` is a means to
  achieve scalability: the premise is that, if needed, more CPUs can be added to servers,
  which the application then automatically starts making use of.**
- applications running on large numbers of servers in the cloud, integrating many systems across many data centers. The ever-increasing demands of end users push the
  requirements of performance and stability of the systems that you build.
  So where are those new concurrency features? Support for concurrency in most
  programming languages, especially on the JVM, has hardly changed. Although the
  implementation details of concurrency APIs have definitely improved, you still have to
  work with low-level constructs like threads and locks, which are notoriously difficult to
  work with. 
- o scaling up (increasing resources; for example, CPUs on existing servers),
  scaling out refers to dynamically adding more servers to a cluster. Since the ’90s, nothing much has changed in how programming languages support networking, either.
  Many technologies still essentially use RPC (remote procedure calls) to communicate
  over the network.
  In the meantime, advances in cloud computing services and multicore CPU architecture have made computing resources ever more abundant.
  PaaS (Platform as a Service) offerings have simplified provisioning and deployment of very large distributed applications, once the domain of only the largest players in the IT industry. Cloud services like AWS EC2 (Amazon Web Services Elastic
  Compute Cloud) and Google Compute Engine give you the ability to literally spin up
  thousands of servers in minutes, while tools like Docker, Puppet, Ansible, and many
  others make it easier to manage and package applications on virtual servers.
  The number of CPU cores in devices is also ever-increasing: even mobile phones
  and tablets have multiple CPU cores today. 
- But that doesn’t mean that you can afford to throw any number of resources at any
  problem. In the end, everything is about cost and efficiency. So it’s all about effectively
  scaling applications, or in other words, getting bang for your buck. Just as you’d never
  use a sorting algorithm with exponential time complexity, it makes sense to think
  about the cost of scaling.

## Why Akka?
- Akka toolkit, an open source project built by Lightbend, provides a simpler, single programming model—one way of coding for concurrent and distributed applications—the actor programming model. Actors are (fitting for
our industry) nothing new at all, in and of themselves. It’s the way that actors are provided in Akka to scale applications both up and out on the JVM that’s unique. As you’ll
see, Akka uses resources efficiently and makes it possible to keep the complexity relatively low while an application scales.
Akka’s primary goal is to make it simpler to build applications that are deployed in
the cloud or run on devices with many cores and that efficiently leverage the full capacity of the computing power available. It’s a toolkit that provides an actor programming
model, runtime, and required supporting tools for building scalable applicat

- First off, Akka is centered on actors. Most of the components in Akka provide support
  in some way for using actors, be it for configuring actors, connecting actors to the network, scheduling actors, or building a cluster out of actors. What makes Akka unique
  is how effortlessly it provides support and additional tooling for building actor-based
  applications, so that you can focus on thinking and programming in actors.
  Briefly, actors are a lot like message queues without the configuration and message
  broker installation overhead. They’re like programmable message queues shrunk to
  microsize—you can easily create thousands, even millions of them. They don’t “do”
  anything unless they’re sent a message.
  Messages are simple data structures that can’t be changed after they’ve been created, or in a single word, they’re immutable. 
- Actors can receive messages one at a time and execute some behavior whenever a
  message is received. Unlike queues, they can also send messages (to other actors).
  Everything an actor does is executed asynchronously. Simply put, you can send a
  message to an actor without waiting for a response. Actors aren’t like threads, but messages sent to them are pushed through on a thread at some point in time. How actors
  are connected to threads is configurable, as you’ll see later; for now it’s good to know
  that this is not a hardwired relationship. 

## The Reactive Manifesto

The Reactive Manifesto (http://www.reactivemanifesto.org/) is an initiative to push for
the design of systems that are more robust, more resilient, more flexible, and better
positioned to meet modern demands. The Akka team has been involved in writing the
Reactive Manifesto from the beginning, and Akka is a product of the ideas that are
expressed in this manifesto.
In short, efficient resource usage and an opportunity for applications to automatically
scale (also called elasticity) is the driver for a big part of the manifesto: 
    
- Blocking I/O limits opportunities for parallelism, so nonblocking I/O is
  preferred. 
- Synchronous interaction limits opportunities for parallelism, so asynchronous
  interaction is preferred. 
- Polling reduces opportunity to use fewer resources, so an event-driven style is
  preferred. 
- If one node can bring down all other nodes, that’s a waste of resources. So
  you need isolation of errors (resilience) to avoid losing all your work.
- Systems need to be elastic: If there’s less demand, you want to use fewer
  resources. If there’s more demand, use more resources, but never more than
  required.
- 
  
  


