lazy val root = (project in file(".")).settings(
 name := "JustinDB-playground",
 version := "0.0.1",
 scalaVersion := "2.12.4",
 libraryDependencies ++= Seq(
  library.crjdt,
  library.crjdtCirce
 )
)


// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library = new {
  object Version {
    final val crjdt = "0.0.7"
  }
  val crjdt      = "eu.timepit" %% "crjdt-core"  % Version.crjdt
  val crjdtCirce = "eu.timepit" %% "crjdt-circe" % Version.crjdt // optional
}