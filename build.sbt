import com.typesafe.sbt.SbtMultiJvm.multiJvmSettings
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

lazy val root = project
  .in(file("."))
  .settings(multiJvmSettings: _*)
  .settings(
    scalaVersion := "2.12.4",
    scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls", "-Xlint"),
    javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    javaOptions in run ++= Seq("-Xms128m", "-Xmx1024m"),
    libraryDependencies ++= Seq(
      library.akkaActor,
      library.akkaRemote,
      library.akkaActor,
      library.akkaCluster,
      library.akkaDistributedData,
      library.akkaMultiNodeTestkit,
      "org.scalatest" %% "scalatest" % "3.0.1" % Test
    ),
    fork in run := true,
    // disable parallel tests
    parallelExecution in Test := false
  )
  .configs (MultiJvm)

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library = new {
  object Version {
    final val crjdt       = "0.0.7"
    final val akkaVersion = "2.5.8"
  }
  val crjdt                = "eu.timepit"        %% "crjdt-core"              % Version.crjdt
  val crjdtCirce           = "eu.timepit"        %% "crjdt-circe"             % Version.crjdt

  val akkaActor            = "com.typesafe.akka" %% "akka-actor"              % Version.akkaVersion
  val akkaRemote           = "com.typesafe.akka" %% "akka-remote"             % Version.akkaVersion
  val akkaCluster          = "com.typesafe.akka" %% "akka-cluster"            % Version.akkaVersion
  val akkaDistributedData  = "com.typesafe.akka" %% "akka-distributed-data"   % Version.akkaVersion
  val akkaMultiNodeTestkit = "com.typesafe.akka" %% "akka-multi-node-testkit" % Version.akkaVersion
}
