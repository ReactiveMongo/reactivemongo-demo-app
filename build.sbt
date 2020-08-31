name := "reactivemongo-demo-app"

//val buildVersion = "0.20.11"
val buildVersion = "1.0.0"

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

scalaVersion := "2.12.11"

libraryDependencies ++= {
  val os = sys.props.get("os.name") match {
    case Some(osx) if osx.toLowerCase.startsWith("mac") =>
      "osx"

    case _ =>
      "linux"
  }

  val (playVer, nativeVer) = buildVersion.split("-").toList match {
    case major :: Nil =>
      s"${major}-play28" -> s"${major}-${os}-x86-64"

    case vs @ _ => {
      val pv = ((vs.init :+ "play28") ++ vs.lastOption.toList)
      val nv = vs match {
        case m :: rc :: Nil if (rc startsWith "rc.") =>
          vs :+ os :+ "x86-64"

        case _ =>
          (vs.init :+ os :+ "x86-64") ++ vs.lastOption.toList
      }

      pv.mkString("-") -> nv.mkString("-")
    }
  }

  val nettyVer = "4.1.44.Final" // same as driver

  Seq(
    guice,
    //"com.typesafe.play" %% "play-iteratees" % "2.6.1",
    "com.typesafe.akka" %% "akka-slf4j" % "2.6.0-M3",
    "org.reactivemongo" %% "play2-reactivemongo" % playVer,
    "io.netty" % "netty-handler" % nettyVer % Runtime,
    "org.reactivemongo" % "reactivemongo-shaded-native" % nativeVer
  )
}

/* Scalafix
inThisBuild(
  List(
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)
 */

import play.sbt.routes.RoutesKeys

RoutesKeys.routesImport += "play.modules.reactivemongo.PathBindables._"

routesGenerator := InjectedRoutesGenerator

fork in run := true

lazy val root = (project in file(".")).enablePlugins(PlayScala)
