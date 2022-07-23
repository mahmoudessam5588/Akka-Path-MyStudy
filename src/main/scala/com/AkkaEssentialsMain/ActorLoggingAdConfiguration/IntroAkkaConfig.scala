package com.AkkaEssentialsMain.ActorLoggingAdConfiguration

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object IntroAkkaConfig extends App {
  class LoggingWithConfigActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case msg => log.info(msg.toString)
    }
  }

  //create introConfig.conf
  //values in the files is key = value pairs
  //also allow nested configuration name spaces with parentheses and values inside
  //how to pass configuration in configuration file
  //a)inline configuration
  //step 1 creating configuration as a string
  val configString =
  """
    |akka{
    |loglevel = "ERROR"
    |}
    |""".stripMargin
  // loglevel can changed as much as we want for example replace "DEBUG" with "INFO"
  //if you restrict the loglevel more to "WARN" OR "ERROR" nothing will be printed to the console
  //unless nasty things Occurred :)
  //step 2 Instantiating ConfigFactory and pass configString to parseString method
  val configFactory = ConfigFactory.parseString(configString)
  // step 3 pass the configFactory Instance to a Construction of an actor system
  val actorsystem = ActorSystem("ConfigurationIntro", ConfigFactory.load(configFactory))
  val actor = actorsystem.actorOf(Props[LoggingWithConfigActor]())
  actor ! "logging & configuration"
  /*prints:
  [DEBUG] [07/23/2022 16:47:52.029] [main] [EventStream(akka://ConfigurationIntro)] logger log1-Logging$DefaultLogger started
  [DEBUG] [07/23/2022 16:47:52.032] [main] [EventStream(akka://ConfigurationIntro)] Default Loggers started
  [DEBUG] [07/23/2022 16:47:52.114] [main] [akka.serialization.Serialization(akka://ConfigurationIntro)] Replacing JavaSerializer with DisabledJavaSerializer, due to `akka.actor.allow-java-serialization = off`.
  [INFO] [07/23/2022 16:47:52.167] [ConfigurationIntro-akka.actor.default-dispatcher-5] [akka://ConfigurationIntro/user/$a] logging & configuration
*/
  //2)configuration in a file (Most Common)
  //destination under /src/main/resources ===> must named application.conf
  //lets create a new actor system with predefined configuration at application.config
  val preDefinedConfiguration = ActorSystem("PreDefinedConfiguration")
  //when you create configuration without providing second argument the akka will automatically go
  // and and search for predefined configuration file and fetch it
  //more importantly akka look by default at certain path /src/main/resources/application.conf
  val preDefinedConfigActor = preDefinedConfiguration.actorOf(Props[LoggingWithConfigActor]())
  preDefinedConfigActor ! "Gone And Fetched The Config From Application File"
  //prints
  //[INFO] [07/23/2022 17:05:58.903] [PreDefinedConfiguration-akka.actor.default-dispatcher-5] [akka://PreDefinedConfiguration/user/$a] Gone And Fetched The Config From Application File

  //As Your Application Go bigger and bigger you need either
  // additional  separate configuration in the same file
  //or separate configuration on separate file
  //3)separate configuration same file
  //after editing our application.conf
  //we create new ActorSystem and pass in config object
  val separateConfig = ConfigFactory.load().getConfig("mySeparateConfig")
  val separateConfigSystem = ActorSystem("SeparateConfigSystem", separateConfig)
  val separateConfigActor = separateConfigSystem.actorOf(Props[LoggingWithConfigActor]())
  separateConfigActor ! "Gone And Fetched Separate Config From Application File"
  //prints
  //[INFO] [07/23/2022 17:25:22.905] [SeparateConfigSystem-akka.actor.default-dispatcher-6] [akka://SeparateConfigSystem/user/$a] Gone And Fetched Separate Config From Application File

  //4)separate Configuration on different file
  //create a subfolder with subfile.conf
  //Instantiate a config factory with load method and add relative path
  val separateConfigFile = ConfigFactory.load("subfolder/subconfigfile.conf")
  val separateConfigSystemFile = ActorSystem("SeparateConfigSeparateFile",separateConfigFile)
  val separateConfigFileActor = separateConfigSystemFile.actorOf(Props[LoggingWithConfigActor]())
  separateConfigFileActor ! "Gone And Fetched Separate Config From Separate File SubFolder/subconfigfile.conf"
  //[INFO] [07/23/2022 17:58:44.836] [SeparateConfigSeparateFile-akka.actor.default-dispatcher-6] [akka://SeparateConfigSeparateFile/user/$a] Gone And Fetched Separate Config From Separate File SubFolder/subconfigfile.conf

  //different file formats
  //eg like Json
  val jsonConfig = ConfigFactory.load("json/jsonConfig.json")
  val jsonConfigActorSystem = ActorSystem("JsonConfigActorSystem",jsonConfig)
  val jsonConfigFileActor = jsonConfigActorSystem.actorOf(Props[LoggingWithConfigActor]())
  jsonConfigFileActor !  "Gone And Fetched Json Config From Separate File json/jsonConfig.json"
  //prints
  //[INFO] [07/23/2022 18:25:23.616] [JsonConfigActorSystem-akka.actor.default-dispatcher-7] [akka://JsonConfigActorSystem/user/$a] Gone And Fetched Json Config From Separate File json/jsonConfig.json
}
