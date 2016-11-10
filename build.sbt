name := "MultiPaxos"

version := "1.0"

scalaVersion := "2.11.8"

val akkaVersion = "2.4.12"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback"    %   "logback-classic"  % "1.1.3"        % Test,
  "com.typesafe.akka" %%  "akka-testkit"     % akkaVersion    % Test,
  "org.scalatest"     %%  "scalatest"        % "2.2.6"        % Test
)

parallelExecution in Test := false