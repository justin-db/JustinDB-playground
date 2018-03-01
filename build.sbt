import com.typesafe.sbt.SbtMultiJvm.multiJvmSettings
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import sbt._
import com.typesafe.sbt.packager.docker._

name           := "justindb-playground"
maintainer     := "mateusz.maciaszekhpc@gmail.com"

// DOCKER DEFINITION
daemonUser.in(Docker) := "root"
maintainer.in(Docker) := "Mateusz Maciaszek"
dockerRepository      := Some("justindb")
dockerUpdateLatest    := true
dockerBaseImage       := "local/openjdk-jre-8-bash"
dockerEntrypoint      ++= Seq(
  """-Dakka.remote.netty.tcp.hostname="$(eval "echo $AKKA_REMOTING_BIND_HOST")"""",
  """-Dakka.remote.netty.tcp.port="$AKKA_REMOTING_BIND_PORT"""",
  """-Djustin.kubernetes-hostname="$(eval "echo $JUSTINDB_NODE_ID_NAME")"""",
  """$(IFS=','; I=0; for NODE in $AKKA_SEED_NODES; do echo "-Dakka.cluster.seed-nodes.$I=akka.tcp://ClusterSystem@$NODE"; I=$(expr $I + 1); done)""",
  "-Dakka.io.dns.resolver=async-dns",
  "-Dakka.io.dns.async-dns.resolve-srv=true",
  "-Dakka.io.dns.async-dns.resolv-conf=on"
)
dockerCommands :=
  dockerCommands.value.flatMap {
    case ExecCmd("ENTRYPOINT", args @ _*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
    case v => Seq(v)
  }

cancelable := true

// PROJECTS
lazy val root = project.in(file("."))
  .enablePlugins(JavaServerAppPackaging)
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
      "org.scalatest" %% "scalatest" % "3.0.1" % Test,
      library.akkaSfl4j,
      library.logback,
      library.scalaLogging,
      "com.lightbend.akka.management" %% "akka-management"              % "0.10.0",
      "com.lightbend.akka.management" %% "akka-management-cluster-http" % "0.10.0"
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
    final val crjdt        = "0.0.7"
    final val akkaVersion  = "2.5.8"
    final val scalaLogging = "3.7.2"
    final val logback      = "1.2.3"

  }
  val crjdt                = "eu.timepit"        %% "crjdt-core"              % Version.crjdt
  val crjdtCirce           = "eu.timepit"        %% "crjdt-circe"             % Version.crjdt

  // logging
  val akkaSfl4j            = "com.typesafe.akka"          %% "akka-slf4j"      % Version.akkaVersion
  val logback              = "ch.qos.logback"              % "logback-classic" % Version.logback
  val scalaLogging         = "com.typesafe.scala-logging" %% "scala-logging"   % Version.scalaLogging

  val akkaActor            = "com.typesafe.akka" %% "akka-actor"              % Version.akkaVersion
  val akkaRemote           = "com.typesafe.akka" %% "akka-remote"             % Version.akkaVersion
  val akkaCluster          = "com.typesafe.akka" %% "akka-cluster"            % Version.akkaVersion
  val akkaDistributedData  = "com.typesafe.akka" %% "akka-distributed-data"   % Version.akkaVersion
  val akkaMultiNodeTestkit = "com.typesafe.akka" %% "akka-multi-node-testkit" % Version.akkaVersion
}
