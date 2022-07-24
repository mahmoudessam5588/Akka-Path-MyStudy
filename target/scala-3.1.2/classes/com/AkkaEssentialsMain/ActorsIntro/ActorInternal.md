# Akka Actor Model Under The Hood

![actor](https://mermaid.ink/img/pako:eNplkMtuwkAMRX_F8iqR4AeyqJQCu1K1KTvCwp0xzCjzQPPoQ0n-vYMCq3p1dX2PZXtE4SVjg2fjv4WikOCl6x2UivnzEuiqoB0GgoMKTBLevDdLu62qxavr9fppemWWj1Arkv5iOHjosptgMxbDh3nh2MlFbODGKYoTbKuj5RjpwqDIScMBOhaayxDLSXl5qv8xu2O1vzPvmTMD7EmbZ_9Tn3CFloMlLcth443sMSm23GNTpKQw9Ni7ueTyVVLindRlQWzOZCKvkHLyH79OYJNC5kdoq6n8w95T8x_cJGfW)

- Akka Has A Thread Pool That Shares With Actors
- One Active Thread Inside Of Thread Pool Is Responsible For Activating An Actor To Run The Code 
- An Actor Is A Passive Data Structure Model Who Has
    
  - A Message Handler == Receive Method handles the messages 
  - Massage Queue == MailBox restore messages send by actor
  
- all the above represent one unit od actor model what happened in real programming
- akka thread pool span number of threads maybe hundreds or even Dozens 
- these (n) Spanned Threads Handles Huge amount of actors equivalent to Millions Of actors per gigabyte of heap
- **_Akka manages to do that by scheduling actors for execution on these Numbers of Threads_**
## Scheduling Mechanism And Communications??

- **Sending Messages:**
  
  - Message Enqueued In Actor's MailBox (Thread Safe)

- **Processing Message**
    
    - Actor is Passive Data Structure So It Needs An Active Running Thread To Activate It 
    - This Is Done By Akka Scheduling A Thread For An Actor
    - Threads Take Control Of The Actor And Start Queuing The Messages One By One
    - For Every Message Dequeued A Massage Handler Invoked
    - This Can Or May Leads To Actors Might Change His State or Sending Messages To Other Actors
    - At After Finishing The Akka Thread Scheduler Decided To Unscheduled The Provided Actor For Execution
    - This Leads To Thread Release Control Of This Actor And Moved To Do New Other Things 

# Guarantees Akka Actor Model Provides

- Guarantees Only One Thread Operates On An Actor On given Time
  - Actors Are Effectively Single Threaded
  - No Locks Needed 
  - All Processing Messages Is Atomic

- Message Delivery Guarantees
  - At Most Once Delivery
  - For Any Sender Receiver Pair The Message Order Is Kept 
