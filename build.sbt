name := "reactivemongo-demo-app"

val buildVersion = "0.20.11"

version := buildVersion

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("staging"))

scalacOptions ++= Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xlint",
  "-g:vars"
  //"-Xfatal-warnings"
)

scalaVersion := "2.13.1"

libraryDependencies ++= {
  val os = sys.props.get("os.name") match {
    case Some(osx) if osx.toLowerCase.startsWith("mac") =>
      "osx"

    case _ =>
      "linux"
  }

  val (playVer, nativeVer) = buildVersion.split("-").toList match {
    case major :: Nil =>
      s"${major}-play27" -> s"${major}-${os}-x86-64"

    case vs @ _ => {
      val pv = ((vs.init :+ "play27") ++ vs.lastOption.toList)
      val nv = ((vs.init :+ os :+ "x86-64") ++ vs.lastOption.toList)

      pv.mkString("-") -> nv.mkString("-")
    }
  }

  Seq(
    guice,
    //"com.typesafe.play" %% "play-iteratees" % "2.6.1",
    "com.typesafe.akka" %% "akka-slf4j" % "2.6.0-M3",
    "org.reactivemongo" %% "play2-reactivemongo" % playVer,
    "org.reactivemongo" % "reactivemongo-shaded-native" % nativeVer
  )
}

routesGenerator := InjectedRoutesGenerator

fork in run := true

lazy val root = (project in file(".")).enablePlugins(PlayScala)
