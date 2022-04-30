name := "reactivemongo-demo-app"

val buildVersion = "1.1.0-RC4"

version := buildVersion

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  Resolver.sonatypeRepo("staging"))

scalacOptions ++= Seq(
  "-encoding", "UTF-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  //"-Xfatal-warnings"
)

scalacOptions ++= {
  if (scalaBinaryVersion.value != "3") {
    Seq(
      "-target:jvm-1.8",
      "-Xlint",
      "-g:vars"
    )
  } else Seq.empty
}

scalaVersion := "3.1.3-RC2"

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
        case m :: rc :: Nil if (rc startsWith "RC") =>
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
    //"com.typesafe.akka" %% "akka-slf4j" % "2.6.18",
    "org.reactivemongo" %% "play2-reactivemongo" % playVer,
    "io.netty" % "netty-handler" % nettyVer % Runtime,
    "org.reactivemongo" % "reactivemongo-shaded-native" % nativeVer
  )
}

libraryDependencies ~= {
  _.map { dep =>
    if (dep.organization == "com.typesafe.play") {
      dep.cross(CrossVersion.for3Use2_13)
    } else {
      dep
    }
  }
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

run / fork := true

lazy val root = (project in file(".")).enablePlugins(PlayScala)
